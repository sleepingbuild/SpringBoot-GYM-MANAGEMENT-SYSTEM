package com.gym.management.service.impl;

import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.dto.response.BookingResponse;
import com.gym.management.dto.response.FaceAttendanceResponse;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.integration.hardware.FaceMatchService;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.BookingService;
import com.gym.management.service.FaceAttendanceService;
import com.gym.management.service.GymCheckInService;
import com.gym.management.service.StaffAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaceAttendanceServiceImpl implements FaceAttendanceService {

    private static final String NO_TRAINER_SESSION_MESSAGE = "Không có buổi nào cần điểm danh";

    private final FaceMatchService faceMatchService;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final StaffAttendanceService staffAttendanceService;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final GymCheckInService gymCheckInService;

    @Value("${gms.face-attendance.min-scan-interval-seconds:120}")
    private long minScanIntervalSeconds;

    // PT có thể dạy 2 buổi liền nhau: quét ra buổi trước, rồi quét tiếp để vào buổi sau.
    // Cửa sổ chống quét đúp 120s sẽ chặn nhầm lượt thứ 2 → PT dùng cửa sổ ngắn hơn, chỉ đủ
    // lọc việc client gửi trùng 1 lần quét (vài giây), không chặn lần quét có chủ đích.
    @Value("${gms.face-attendance.trainer-min-scan-interval-seconds:15}")
    private long trainerMinScanIntervalSeconds;

    @Override
    public FaceAttendanceResponse kioskScan(FaceDescriptorRequest request) {
        UUID matchedUserId = faceMatchService.matchAgainstAll(request.getDescriptor())
                .orElseThrow(() -> new BusinessException(ErrorCode.FACE_NOT_RECOGNIZED,
                        "Không nhận diện được khuôn mặt"));
        return processScan(matchedUserId);
    }

    @Override
    public FaceAttendanceResponse selfScan(UUID currentUserId, FaceDescriptorRequest request) {
        boolean matched = faceMatchService.matchAgainstUser(request.getDescriptor(), currentUserId);
        if (!matched) {
            throw new BusinessException(ErrorCode.FACE_NOT_RECOGNIZED,
                    "Khuôn mặt không khớp với tài khoản đang đăng nhập");
        }
        return processScan(currentUserId);
    }

    private FaceAttendanceResponse processScan(UUID matchedUserId) {
        User user = userRepository.findById(matchedUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FACE_PROFILE_NOT_FOUND));

        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        // Chống quét đúp (quyết định A.3): SETNX atomic ở Redis. PT dùng cửa sổ ngắn hơn
        // (xem ghi chú ở trainerMinScanIntervalSeconds).
        long intervalSeconds = roleNames.contains("TRAINER")
                ? trainerMinScanIntervalSeconds
                : minScanIntervalSeconds;
        String dedupKey = "face-scan:" + matchedUserId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(intervalSeconds));

        if (Boolean.FALSE.equals(acquired)) {
            return FaceAttendanceResponse.builder()
                    .matchedUserId(matchedUserId)
                    .matchedUserName(user.getFullName())
                    .action("NONE")
                    .targetType(null)
                    .scanTime(LocalDateTime.now(clock))
                    .bookingLinked(false)
                    .duplicateScan(true)
                    .build();
        }

        try {
            return transactionTemplate.execute(status -> doProcessScan(user, roleNames));
        } catch (RuntimeException e) {
            // Lượt quét bị từ chối (ví dụ MEMBERSHIP_EXPIRED): giải phóng khóa để hội viên quét lại ngay sau khi xử lý xong.
            redisTemplate.delete(dedupKey);
            throw e;
        }
    }

    private FaceAttendanceResponse doProcessScan(User user, Set<String> roleNames) {
        UUID matchedUserId = user.getId();
        LocalDateTime now = LocalDateTime.now(clock);

        if (roleNames.contains("MEMBER")) {
            Optional<BookingResponse> booking = bookingService.recordFaceScan(matchedUserId, now);
            if (booking.isPresent()) {
                // Có buổi PT phù hợp: chỉ cập nhật bookings, KHÔNG ghi gym_check_ins (CH2,
                // tránh đếm hai lần trong báo cáo lưu lượng).
                String action = booking.get().getCheckOutTime() != null ? "CHECK_OUT" : "CHECK_IN";
                return FaceAttendanceResponse.builder()
                        .matchedUserId(matchedUserId)
                        .matchedUserName(user.getFullName())
                        .action(action)
                        .targetType("BOOKING")
                        .scanTime(now)
                        .bookingLinked(true)
                        .duplicateScan(false)
                        .build();
            }

            // Không có buổi PT phù hợp: ghi lượt vào/ra phòng tập (CH2 / V8).
            GymCheckInService.ScanResult result = gymCheckInService.recordFaceScan(matchedUserId, now);
            return FaceAttendanceResponse.builder()
                    .matchedUserId(matchedUserId)
                    .matchedUserName(user.getFullName())
                    .action(result.action())
                    .targetType("GYM_CHECK_IN")
                    .scanTime(now)
                    .bookingLinked(false)
                    .duplicateScan(false)
                    .build();
        }

        // G3 (CHANGE_PT_ATTENDANCE_AND_PAY.md): PT quét mặt chỉ ghi vào Booking, KHÔNG ghi
        // staff_attendances nữa (PT trả lương theo buổi).
        if (roleNames.contains("TRAINER")) {
            return processTrainerScan(user, now);
        }

        // Phi chốt (2026-10): Lễ tân và Sales đều chấm công khuôn mặt vào staff_attendances.
        if (roleNames.contains("RECEPTIONIST") || roleNames.contains("SALES")) {
            var result = staffAttendanceService.recordScan(matchedUserId, now, "FACE");
            return FaceAttendanceResponse.builder()
                    .matchedUserId(matchedUserId)
                    .matchedUserName(user.getFullName())
                    .action(result.getCheckOutTime() != null ? "CHECK_OUT" : "CHECK_IN")
                    .targetType("STAFF_ATTENDANCE")
                    .scanTime(now)
                    .bookingLinked(true)
                    .duplicateScan(false)
                    .build();
        }

        throw new BusinessException(ErrorCode.FACE_NOT_RECOGNIZED,
                "Vai trò người dùng không hỗ trợ điểm danh khuôn mặt");
    }

    private FaceAttendanceResponse processTrainerScan(User trainer, LocalDateTime now) {
        // `var` để không phụ thuộc vị trí khai báo của TrainerScanResult (Agent 3)
        var scan = bookingService.recordTrainerFaceScan(trainer.getId(), now);

        if (scan.isEmpty()) {
            // Không có buổi phù hợp: KHÔNG phải lỗi, chỉ báo cho FE hiển thị
            return FaceAttendanceResponse.builder()
                    .matchedUserId(trainer.getId())
                    .matchedUserName(trainer.getFullName())
                    .action("NONE")
                    .targetType("BOOKING")
                    .scanTime(now)
                    .bookingLinked(false)
                    .duplicateScan(false)
                    .message(NO_TRAINER_SESSION_MESSAGE)
                    .build();
        }

        var result = scan.get();
        // earlyLeave chỉ có nghĩa khi check-out (quét ra trước giờ kết thúc buổi)
        boolean earlyLeave = "CHECK_OUT".equals(result.type()) && result.earlyLeave();

        return FaceAttendanceResponse.builder()
                .matchedUserId(trainer.getId())
                .matchedUserName(trainer.getFullName())
                .action(result.type())
                .targetType("BOOKING")
                .scanTime(now)
                .bookingLinked(true)
                .duplicateScan(false)
                .bookingId(result.booking().getId())
                .earlyLeave(earlyLeave)
                .reasonRequired(earlyLeave)
                .build();
    }
}

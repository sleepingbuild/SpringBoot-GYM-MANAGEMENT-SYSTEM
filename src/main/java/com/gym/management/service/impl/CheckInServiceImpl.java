package com.gym.management.service.impl;

import com.gym.management.dto.response.CheckInResponse;
import com.gym.management.entity.Branch;
import com.gym.management.entity.CheckIn;
import com.gym.management.entity.MemberPackage;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.integration.hardware.FaceMatcher;
import com.gym.management.repository.BranchRepository;
import com.gym.management.repository.CheckInRepository;
import com.gym.management.repository.MemberPackageRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent 3 - Check-in Engine & Access Control.
 *
 * Cài đặt đúng 6 bước mô tả trong ARCHITECTURE.md mục 3.1, chỉ khác điểm khởi
 * đầu: thay vì nhận card/QR id, nhận diện user qua {@link FaceMatcher} trước
 * (bước 0), rồi mới chạy tiếp 6 bước như luồng chung.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final FaceMatcher faceMatcher;
    private final UserRepository userRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final CheckInRepository checkInRepository;
    private final BranchRepository branchRepository;

    @Value("${gms.checkin.anti-passback-window-seconds:300}")
    private long antiPassbackWindowSeconds;

    @Value("${gms.checkin.off-peak-start-hour:8}")
    private int offPeakStartHour;

    @Value("${gms.checkin.off-peak-end-hour:15}")
    private int offPeakEndHour;

    @Override
    @Transactional
    public CheckInResponse checkInByFace(byte[] imageBytes, UUID branchId, String deviceId) {

        // Bước 0 (riêng của phương thức FACE): nhận diện user từ ảnh camera.
        // Nếu không nhận ra ai cả thì KHÔNG có user_id để ghi log check_ins (cột NOT NULL)
        // nên chỉ log cảnh báo hệ thống, không tạo bản ghi check-in.
        UUID userId = faceMatcher.identify(imageBytes)
                .orElseThrow(() -> {
                    log.warn("[CheckIn][FACE] Không nhận diện được khuôn mặt trong ảnh gửi lên (branchId={}, deviceId={})", branchId, deviceId);
                    return new BusinessException(ErrorCode.FACE_NOT_RECOGNIZED,
                            "Không nhận diện được khuôn mặt. Vui lòng thử lại hoặc dùng phương thức khác.");
                });

        // Bước 1: user có tồn tại? (đã có userId từ face match nên chắc chắn tồn tại,
        // nhưng vẫn load lại để lấy fullName + đảm bảo chưa bị xoá/khoá)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_USER_NOT_FOUND));

        Branch branch = branchId != null ? branchRepository.findById(branchId).orElse(null) : null;

        // Bước 2: tìm MemberPackage đang ACTIVE gần nhất
        Optional<MemberPackage> activePackageOpt = memberPackageRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE")
                .stream().findFirst();

        if (activePackageOpt.isEmpty()) {
            saveDenied(user, branch, deviceId, "DENIED_NO_ACTIVE_PACKAGE", null);
            throw new BusinessException(ErrorCode.CHECKIN_NO_ACTIVE_PACKAGE, "Bạn chưa có gói tập nào đang hoạt động");
        }
        MemberPackage memberPackage = activePackageOpt.get();

        // Bước 3: kiểm tra thời hạn (start_date <= today <= end_date, end_date null = không giới hạn ngày)
        LocalDate today = LocalDate.now(VN_ZONE);
        boolean withinDateRange = !today.isBefore(memberPackage.getStartDate())
                && (memberPackage.getEndDate() == null || !today.isAfter(memberPackage.getEndDate()));
        if (!withinDateRange) {
            saveDenied(user, branch, deviceId, "DENIED_EXPIRED", memberPackage);
            throw new BusinessException(ErrorCode.MEMBERSHIP_EXPIRED, "Gói tập đã hết hạn");
        }

        // Bước 4: nếu gói theo lượt (SESSION_BASED), kiểm tra remaining_sessions > 0
        String packageType = memberPackage.getGymPackage().getPackageType();
        if ("SESSION_BASED".equals(packageType)) {
            Integer remaining = memberPackage.getRemainingSessions();
            if (remaining == null || remaining <= 0) {
                saveDenied(user, branch, deviceId, "DENIED_NO_SESSION", memberPackage);
                throw new BusinessException(ErrorCode.MEMBERSHIP_NO_SESSION, "Bạn đã hết số buổi tập trong gói");
            }
        }

        // Bước 5: kiểm tra khung giờ (Off-peak: chỉ tập offPeakStartHour-offPeakEndHour)
        String peakType = memberPackage.getGymPackage().getPeakType();
        if ("OFF_PEAK".equals(peakType)) {
            LocalTime now = LocalTime.now(VN_ZONE);
            boolean withinOffPeak = !now.isBefore(LocalTime.of(offPeakStartHour, 0))
                    && now.isBefore(LocalTime.of(offPeakEndHour, 0));
            if (!withinOffPeak) {
                saveDenied(user, branch, deviceId, "DENIED_TIME", memberPackage);
                throw new BusinessException(ErrorCode.CHECKIN_OUT_OF_TIME_RANGE,
                        String.format("Gói Off-peak chỉ được tập từ %02d:00 đến %02d:00", offPeakStartHour, offPeakEndHour));
            }
        }

        // Bước 6: Anti-passback — khoảng cách với lần check-in SUCCESS gần nhất phải > ngưỡng cấu hình
        Optional<CheckIn> lastSuccess = checkInRepository.findFirstByUserIdAndStatusOrderByCheckinTimeDesc(userId, "SUCCESS");
        if (lastSuccess.isPresent()) {
            long secondsSinceLast = Instant.now().getEpochSecond() - lastSuccess.get().getCheckinTime().getEpochSecond();
            if (secondsSinceLast < antiPassbackWindowSeconds) {
                saveDenied(user, branch, deviceId, "DENIED_ANTI_PASSBACK", memberPackage);
                throw new BusinessException(ErrorCode.CHECKIN_ANTI_PASSBACK,
                        "Bạn vừa check-in cách đây chưa đủ " + (antiPassbackWindowSeconds / 60) + " phút");
            }
        }

        // HỢP LỆ: trừ buổi (nếu SESSION_BASED) rồi ghi log SUCCESS
        if ("SESSION_BASED".equals(packageType)) {
            memberPackage.setRemainingSessions(memberPackage.getRemainingSessions() - 1);
            memberPackageRepository.save(memberPackage);
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setUser(user);
        checkIn.setMemberPackage(memberPackage);
        checkIn.setBranch(branch);
        checkIn.setMethod("FACE");
        checkIn.setStatus("SUCCESS");
        checkIn.setDeviceId(deviceId);
        checkInRepository.save(checkIn);

        log.info("[CheckIn][FACE] SUCCESS user={} package={}", userId, memberPackage.getId());

        return toResponse(checkIn, user);
    }

    @Override
    public List<CheckInResponse> getHistoryForUser(UUID userId) {
        return checkInRepository.findByUserIdOrderByCheckinTimeDesc(userId).stream()
                .map(c -> toResponse(c, c.getUser()))
                .toList();
    }

    @Override
    public List<CheckInResponse> getHistoryForBranch(UUID branchId) {
        return checkInRepository.findByBranchIdOrderByCheckinTimeDesc(branchId).stream()
                .map(c -> toResponse(c, c.getUser()))
                .toList();
    }

    private void saveDenied(User user, Branch branch, String deviceId, String status, MemberPackage memberPackage) {
        CheckIn checkIn = new CheckIn();
        checkIn.setUser(user);
        checkIn.setBranch(branch);
        checkIn.setMemberPackage(memberPackage);
        checkIn.setMethod("FACE");
        checkIn.setStatus(status);
        checkIn.setDeviceId(deviceId);
        checkInRepository.save(checkIn);
        log.info("[CheckIn][FACE] {} user={}", status, user.getId());
    }

    private CheckInResponse toResponse(CheckIn checkIn, User user) {
        Integer remaining = checkIn.getMemberPackage() != null ? checkIn.getMemberPackage().getRemainingSessions() : null;
        return new CheckInResponse(
                checkIn.getId(),
                user.getId(),
                user.getFullName(),
                checkIn.getMethod(),
                checkIn.getStatus(),
                checkIn.getCheckinTime(),
                remaining
        );
    }
}

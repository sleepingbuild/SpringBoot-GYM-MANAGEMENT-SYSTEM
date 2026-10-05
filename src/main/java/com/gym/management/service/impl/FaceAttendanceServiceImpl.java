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
import com.gym.management.service.StaffAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaceAttendanceServiceImpl implements FaceAttendanceService {

    private final FaceMatchService faceMatchService;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final StaffAttendanceService staffAttendanceService;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${gms.face-attendance.min-scan-interval-seconds:120}")
    private long minScanIntervalSeconds;

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

        // Chống quét đúp (quyết định A.3 của Phi/Agent 1): 1 user chỉ được xử lý 1 lần mỗi
        // gms.face-attendance.min-scan-interval-seconds. SETNX atomic ở Redis nên đúng cả khi
        // race đồng thời lẫn khi 2 request cách nhau vài giây. Thay thế userLocks in-memory cũ.
        String dedupKey = "face-scan:" + matchedUserId;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(minScanIntervalSeconds));

        if (Boolean.FALSE.equals(acquired)) {
            return FaceAttendanceResponse.builder()
                    .matchedUserId(matchedUserId)
                    .matchedUserName(user.getFullName())
                    .action("NONE")
                    .targetType(null)
                    .scanTime(LocalDateTime.now())
                    .bookingLinked(false)
                    .duplicateScan(true)
                    .build();
        }

        return transactionTemplate.execute(status -> doProcessScan(user));
    }

    private FaceAttendanceResponse doProcessScan(User user) {
        UUID matchedUserId = user.getId();
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();

        if (roleNames.contains("MEMBER")) {
            Optional<BookingResponse> booking = bookingService.recordFaceScan(matchedUserId, now);
            boolean linked = booking.isPresent();
            String action = linked && booking.get().getCheckOutTime() != null ? "CHECK_OUT" : "CHECK_IN";
            return FaceAttendanceResponse.builder()
                    .matchedUserId(matchedUserId)
                    .matchedUserName(user.getFullName())
                    .action(action)
                    .targetType("BOOKING")
                    .scanTime(now)
                    .bookingLinked(linked)
                    .duplicateScan(false)
                    .build();
        }

        if (roleNames.contains("TRAINER") || roleNames.contains("RECEPTIONIST")) {
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
}

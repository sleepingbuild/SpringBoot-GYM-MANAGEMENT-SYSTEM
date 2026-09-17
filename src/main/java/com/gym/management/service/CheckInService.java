package com.gym.management.service;

import com.gym.management.dto.response.CheckInResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CheckInService {

    /**
     * Check-in bằng khuôn mặt: nhận diện user từ ảnh camera (1:N qua FaceMatcher),
     * sau đó chạy đủ luồng 6 bước xác thực gói tập (xem CheckInServiceImpl).
     */
    CheckInResponse checkInByFace(byte[] imageBytes, UUID branchId, String deviceId);

    List<CheckInResponse> getHistoryForUser(UUID userId);

    List<CheckInResponse> getHistoryForBranch(UUID branchId);
}

package com.gym.management.service;

import com.gym.management.dto.response.FaceProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FaceProfileService {

    /**
     * Admin/Lễ tân đăng ký (hoặc thay thế) ảnh khuôn mặt cho 1 hội viên/PT.
     *
     * @param targetUserId  user được đăng ký ảnh
     * @param image         ảnh chụp/tải lên
     * @param registeredById user đang thực hiện thao tác (Admin/Lễ tân đang đăng nhập)
     */
    FaceProfileResponse registerOrReplace(UUID targetUserId, MultipartFile image, UUID registeredById);

    FaceProfileResponse getStatus(UUID targetUserId);

    void delete(UUID targetUserId);
}

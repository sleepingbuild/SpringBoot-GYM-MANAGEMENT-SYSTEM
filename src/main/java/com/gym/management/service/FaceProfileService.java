package com.gym.management.service;

import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.dto.response.FaceProfileResponse;

import java.util.UUID;

public interface FaceProfileService {

    /** Member/Trainer tự đăng ký — descriptor trích ở client */
    FaceProfileResponse registerSelf(UUID currentUserId, FaceDescriptorRequest request);

    /**
     * Lễ tân/Admin đăng ký hộ — descriptor trích ở client bằng face-api.js chạy trên ảnh
     * người dùng chọn (quyết định A.1: không có endpoint ảnh/multipart, backend chỉ nhận JSON
     * descriptor giống hệt registerSelf, chỉ khác targetUserId != registeredBy).
     */
    FaceProfileResponse registerFor(UUID targetUserId, UUID registeredBy, FaceDescriptorRequest request);

    FaceProfileResponse getByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}

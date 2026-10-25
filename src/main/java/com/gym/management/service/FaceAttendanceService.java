package com.gym.management.service;

import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.dto.response.FaceAttendanceResponse;

import java.util.UUID;

public interface FaceAttendanceService {

    /** Luồng Kiosk — Lễ tân/Admin, so 1:N với toàn bộ hồ sơ */
    FaceAttendanceResponse kioskScan(FaceDescriptorRequest request);

    /** Luồng tự điểm danh — Member/Trainer, so 1:1 với chính currentUserId */
    FaceAttendanceResponse selfScan(UUID currentUserId, FaceDescriptorRequest request);
}

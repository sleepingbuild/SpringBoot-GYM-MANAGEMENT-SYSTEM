package com.gym.management.controller;

import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.FaceAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Face Attendance", description = "Điểm danh bằng khuôn mặt (Kiosk 1:N, tự điểm danh 1:1)")
@RestController
@RequestMapping("/api/v1/face-attendance")
@RequiredArgsConstructor
public class FaceAttendanceController {

    private final FaceAttendanceService faceAttendanceService;

    @Operation(summary = "Điểm danh qua Kiosk (so khớp 1:N)",
            description = "Lễ tân/Admin vận hành. Hệ thống ghi theo role của người được nhận diện: "
                    + "MEMBER/TRAINER → booking, RECEPTIONIST/SALES → chấm công nhân sự.")
    @PostMapping("/kiosk")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> kiosk(@Valid @RequestBody FaceDescriptorRequest request) {
        return ApiResponse.success(faceAttendanceService.kioskScan(request));
    }

    @Operation(summary = "Tự điểm danh (so khớp 1:1 với tài khoản đang đăng nhập)",
            description = "Dành cho MEMBER, TRAINER, RECEPTIONIST, SALES. Lần quét đầu trong ngày/buổi là vào, lần sau là ra.")
    @PostMapping("/self")
    @PreAuthorize("hasAnyRole('MEMBER', 'TRAINER', 'RECEPTIONIST', 'SALES')")
    public ApiResponse<?> self(@Valid @RequestBody FaceDescriptorRequest request) {
        return ApiResponse.success(faceAttendanceService.selfScan(CurrentUserId.get(), request));
    }
}

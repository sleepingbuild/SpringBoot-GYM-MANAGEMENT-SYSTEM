package com.gym.management.controller;

import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.FaceAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/face-attendance")
@RequiredArgsConstructor
public class FaceAttendanceController {

    private final FaceAttendanceService faceAttendanceService;

    @PostMapping("/kiosk")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> kiosk(@Valid @RequestBody FaceDescriptorRequest request) {
        return ApiResponse.success(faceAttendanceService.kioskScan(request));
    }

    @PostMapping("/self")
    @PreAuthorize("hasAnyRole('MEMBER', 'TRAINER')")
    public ApiResponse<?> self(@Valid @RequestBody FaceDescriptorRequest request) {
        return ApiResponse.success(faceAttendanceService.selfScan(CurrentUserId.get(), request));
    }
}

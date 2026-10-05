package com.gym.management.controller;

import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.FaceProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/face-profiles")
@RequiredArgsConstructor
public class FaceProfileController {

    private final FaceProfileService faceProfileService;

    @PostMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'TRAINER')")
    public ApiResponse<?> registerSelf(@Valid @RequestBody FaceDescriptorRequest request) {
        return ApiResponse.success("Đăng ký khuôn mặt thành công",
                faceProfileService.registerSelf(CurrentUserId.get(), request));
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> registerFor(@PathVariable UUID userId,
                                       @Valid @RequestBody FaceDescriptorRequest request) {
        return ApiResponse.success("Đăng ký hộ khuôn mặt thành công",
                faceProfileService.registerFor(userId, CurrentUserId.get(), request));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> get(@PathVariable UUID userId) {
        return ApiResponse.success(faceProfileService.getByUserId(userId));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> delete(@PathVariable UUID userId) {
        faceProfileService.deleteByUserId(userId);
        return ApiResponse.success("Đã xoá hồ sơ khuôn mặt", null);
    }
}

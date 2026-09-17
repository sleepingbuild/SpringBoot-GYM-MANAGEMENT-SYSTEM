package com.gym.management.controller;

import com.gym.management.dto.response.FaceProfileResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUser;
import com.gym.management.service.FaceProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Agent 3 (đăng ký ảnh) — thao tác bởi Admin/Lễ tân theo đúng yêu cầu:
 * "admin gửi ảnh lên DB trước rồi khách hàng/PT chỉ cần quét cam".
 */
@RestController
@RequestMapping("/api/v1/members/{userId}/face-profile")
@RequiredArgsConstructor
@Tag(name = "Face Profile", description = "Đăng ký ảnh khuôn mặt cho hội viên/PT (Admin/Lễ tân thao tác)")
public class FaceProfileController {

    private final FaceProfileService faceProfileService;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Đăng ký (hoặc thay thế) ảnh khuôn mặt cho 1 hội viên/PT")
    public ApiResponse<FaceProfileResponse> register(@PathVariable UUID userId,
                                                       @RequestParam("image") MultipartFile image,
                                                       @CurrentUser UUID currentUserId) {
        return ApiResponse.success("Đăng ký ảnh khuôn mặt thành công",
                faceProfileService.registerOrReplace(userId, image, currentUserId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Kiểm tra hội viên/PT đã đăng ký ảnh khuôn mặt chưa")
    public ApiResponse<FaceProfileResponse> getStatus(@PathVariable UUID userId) {
        return ApiResponse.success(faceProfileService.getStatus(userId));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Xoá ảnh khuôn mặt đã đăng ký (vd hội viên yêu cầu đăng ký lại)")
    public ApiResponse<Void> delete(@PathVariable UUID userId) {
        faceProfileService.delete(userId);
        return ApiResponse.success("Đã xoá ảnh khuôn mặt", null);
    }
}

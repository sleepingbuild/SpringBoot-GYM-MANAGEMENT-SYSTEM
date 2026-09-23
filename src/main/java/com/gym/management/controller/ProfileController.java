package com.gym.management.controller;

import com.gym.management.dto.request.UpdateProfileRequest;
import com.gym.management.dto.response.ProfileResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.security.CurrentUser;
import com.gym.management.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Agent 1 - Module Hồ sơ cá nhân (Member + Trainer).
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Hồ sơ cá nhân — tuổi, cân nặng, chiều cao, mục tiêu, avatar")
public class ProfileController {

    private final ProfileService profileService;

    @Value("${gms.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping("/me")
    @Operation(summary = "Xem hồ sơ cá nhân của chính mình")
    public ApiResponse<ProfileResponse> getMyProfile(@CurrentUser UUID currentUserId) {
        return ApiResponse.success(profileService.getMyProfile(currentUserId));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật tuổi/cân nặng/chiều cao/mục tiêu")
    public ApiResponse<ProfileResponse> updateMyProfile(@CurrentUser UUID currentUserId,
                                                          @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Cập nhật hồ sơ thành công", profileService.updateMyProfile(currentUserId, request));
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    @Operation(summary = "Tải lên ảnh đại diện")
    public ApiResponse<ProfileResponse> uploadAvatar(@CurrentUser UUID currentUserId,
                                                       @RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng chọn ảnh");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "File tải lên phải là ảnh");
        }

        String extension = "";
        String original = image.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID() + extension;

        try {
            Path avatarDir = Path.of(uploadDir, "avatars");
            Files.createDirectories(avatarDir);
            Files.copy(image.getInputStream(), avatarDir.resolve(filename));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Không lưu được ảnh");
        }

        String avatarUrl = "/uploads/avatars/" + filename;
        return ApiResponse.success("Cập nhật avatar thành công", profileService.updateAvatar(currentUserId, avatarUrl));
    }
}

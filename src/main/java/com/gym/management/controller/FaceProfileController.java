package com.gym.management.controller;

import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.FaceProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Face Profiles", description = "Đăng ký và quản lý hồ sơ khuôn mặt (descriptor 128 chiều, không lưu ảnh)")
@RestController
@RequestMapping("/api/v1/face-profiles")
@RequiredArgsConstructor
public class FaceProfileController {

    private final FaceProfileService faceProfileService;

    @Operation(summary = "Tự đăng ký hồ sơ khuôn mặt",
            description = "Client trích descriptor 128 chiều bằng face-api.js rồi gửi lên. Đăng ký lại sẽ ghi đè hồ sơ cũ.")
    @PostMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'TRAINER', 'RECEPTIONIST', 'SALES')")
    public ApiResponse<?> registerSelf(@Valid @RequestBody FaceDescriptorRequest request) {
        return ApiResponse.success("Đăng ký khuôn mặt thành công",
                faceProfileService.registerSelf(CurrentUserId.get(), request));
    }

    @Operation(summary = "Đăng ký hộ hồ sơ khuôn mặt cho người dùng khác",
            description = "Lễ tân/Admin chọn ảnh trong trình duyệt, face-api.js trích descriptor, backend chỉ nhận JSON descriptor.")
    @PostMapping("/{userId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> registerFor(@PathVariable UUID userId,
                                       @Valid @RequestBody FaceDescriptorRequest request) {
        return ApiResponse.success("Đăng ký hộ khuôn mặt thành công",
                faceProfileService.registerFor(userId, CurrentUserId.get(), request));
    }

    @Operation(summary = "Xem hồ sơ khuôn mặt của một người dùng")
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> get(@PathVariable UUID userId) {
        return ApiResponse.success(faceProfileService.getByUserId(userId));
    }

    @Operation(summary = "Xoá hồ sơ khuôn mặt của một người dùng")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> delete(@PathVariable UUID userId) {
        faceProfileService.deleteByUserId(userId);
        return ApiResponse.success("Đã xoá hồ sơ khuôn mặt", null);
    }
}

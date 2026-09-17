package com.gym.management.controller;

import com.gym.management.dto.response.CheckInResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.security.CurrentUser;
import com.gym.management.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Agent 3 - Check-in Engine & Access Control.
 */
@RestController
@RequestMapping("/api/v1/checkin")
@RequiredArgsConstructor
@Tag(name = "Check-in", description = "Điểm danh ra/vào phòng gym")
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * Endpoint dành cho camera/kiosk đặt tại cửa ra vào — KHÔNG yêu cầu đăng nhập
     * (giống như thiết bị QR/RFID vật lý), vì danh tính được xác định qua khuôn mặt,
     * không qua JWT. Xem SecurityConfig.PUBLIC_ENDPOINTS.
     */
    @PostMapping(value = "/face", consumes = "multipart/form-data")
    @Operation(summary = "Check-in bằng khuôn mặt (camera tại quầy/kiosk quét trực tiếp)")
    public ApiResponse<CheckInResponse> checkInByFace(@RequestParam("image") MultipartFile image,
                                                        @RequestParam(required = false) UUID branchId,
                                                        @RequestParam(required = false) String deviceId) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.FACE_IMAGE_INVALID, "Vui lòng gửi ảnh chụp từ camera");
        }
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FACE_IMAGE_INVALID, "Không đọc được ảnh gửi lên");
        }
        return ApiResponse.success("Check-in thành công", checkInService.checkInByFace(bytes, branchId, deviceId));
    }

    @GetMapping("/history/me")
    @Operation(summary = "Lịch sử check-in của chính hội viên/PT đang đăng nhập")
    public ApiResponse<List<CheckInResponse>> myHistory(@CurrentUser UUID currentUserId) {
        return ApiResponse.success(checkInService.getHistoryForUser(currentUserId));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RECEPTIONIST')")
    @Operation(summary = "Lịch sử check-in theo chi nhánh (Lễ tân/Admin)")
    public ApiResponse<List<CheckInResponse>> branchHistory(@RequestParam UUID branchId) {
        return ApiResponse.success(checkInService.getHistoryForBranch(branchId));
    }
}

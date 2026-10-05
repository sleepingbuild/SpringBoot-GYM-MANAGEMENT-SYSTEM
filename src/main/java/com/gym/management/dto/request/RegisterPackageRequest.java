package com.gym.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * POST /api/v1/member-packages — đăng ký gói MỚI HOÀN TOÀN (không phải nâng/hạ cấp,
 * xem UpgradeDowngradeRequest cho 2 luồng đó).
 */
@Getter
@Setter
public class RegisterPackageRequest {

    /**
     * Chỉ có giá trị khi Lễ tân/Admin đăng ký hộ cho hội viên khác — Member tự mua
     * thì để trống (Controller tự lấy userId từ người đang đăng nhập).
     */
    private UUID userId;

    @NotNull(message = "packageId không được để trống")
    private UUID packageId;

    private UUID branchId;
}

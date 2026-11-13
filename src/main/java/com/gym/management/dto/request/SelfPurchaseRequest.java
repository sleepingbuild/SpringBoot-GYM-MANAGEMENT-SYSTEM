package com.gym.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Hội viên tự mua gói. Cố ý KHÔNG có userId: người mua luôn là người đang đăng nhập (lấy từ token),
 * để không thể mua hộ hay mua nhầm cho người khác qua body.
 */
@Getter
@Setter
@NoArgsConstructor
public class SelfPurchaseRequest {

    @NotNull(message = "Vui lòng chọn gói tập")
    private UUID packageId;

    /** Không bắt buộc: để trống = không gắn chi nhánh (giống đăng ký thường). */
    private UUID branchId;
}

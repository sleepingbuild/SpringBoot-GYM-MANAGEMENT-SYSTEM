package com.gym.management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Agent 5 - Đơn do hội viên tự mua. Cố ý KHÔNG có memberId (người mua luôn lấy từ JWT) và KHÔNG có
 * paymentMethod (thanh toán mô phỏng cố định), nên client không thể mua hộ hay đặt phương thức thanh toán.
 */
@Getter
@Setter
public class MemberPosOrderRequest {

    /** Chi nhánh nhận hàng, tuỳ chọn. */
    private UUID branchId;

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Size(max = 100, message = "Đơn hàng tối đa 100 dòng")
    @Valid
    private List<PosOrderCreateRequest.Item> items;
}

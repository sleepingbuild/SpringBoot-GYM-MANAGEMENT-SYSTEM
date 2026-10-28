package com.gym.management.service;

import com.gym.management.dto.request.PosOrderCreateRequest;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.PosOrderResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Agent 5 - POS & Commission Module.
 */
public interface PosOrderService {

    /**
     * Tạo đơn + xác nhận thanh toán ngay trong cùng 1 transaction (bán hàng tại quầy không
     * có bước "chờ xác nhận" như Membership — tiền đã thu ngay lúc lễ tân bấm tạo đơn).
     * user của giao dịch thanh toán = người mua (hội viên, null nếu khách vãng lai), không phải
     * nhân viên thu tiền (nhân viên nằm ở pos_orders.staff_id). Đơn khách vãng lai chỉ ghi được
     * giao dịch sau khi CH1/V7 (payment_transactions.user_id cho phép NULL) của Agent 2 lên master.
     * Giá lấy từ DB, KHÔNG tin unit_price client gửi (nếu client có gửi). Trừ tồn kho
     * nguyên tử bằng khoá bi quan trên từng dòng sản phẩm (xem ProductRepository#findByIdForUpdate).
     */
    PosOrderResponse createOrder(UUID staffId, PosOrderCreateRequest request);

    /** Huỷ đơn COMPLETED, hoàn lại tồn kho cho mọi dòng sản phẩm trong đơn. Idempotent-safe. */
    PosOrderResponse cancelOrder(UUID id);

    PosOrderResponse getById(UUID id);

    PageResponse<PosOrderResponse> search(UUID branchId, LocalDate from, LocalDate to, Pageable pageable);
}

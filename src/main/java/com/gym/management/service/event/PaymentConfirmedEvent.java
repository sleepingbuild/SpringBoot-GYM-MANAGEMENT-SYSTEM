package com.gym.management.service.event;

import java.util.UUID;

/**
 * Phát ra khi 1 payment_transaction chuyển PENDING → SUCCESS LẦN ĐẦU TIÊN
 * ({@code PaymentService#confirmPayment}). Xác nhận lặp lại khi đã SUCCESS KHÔNG phát lại.
 *
 * Tại sao là sự kiện thay vì gọi thẳng: Membership đã phụ thuộc Payment (tạo giao dịch PENDING),
 * nếu Payment lại inject Membership để kích hoạt gói thì 2 bean inject constructor lẫn nhau và
 * ứng dụng không khởi động được (vòng phụ thuộc). Payment chỉ phát sự kiện, không biết ai lắng nghe.
 *
 * <ul>
 *   <li>{@code relatedType = MEMBERSHIP}: {@code MembershipPaymentListener} kích hoạt gói
 *       ({@code relatedId} = id member_packages).</li>
 *   <li>{@code relatedType = POS}: để module POS (Agent 5) tự lắng nghe nếu cần, module này không xử lý.</li>
 * </ul>
 *
 * Listener là {@code @EventListener} đồng bộ, chạy chung transaction với {@code confirmPayment}:
 * listener ném lỗi thì cả việc xác nhận thanh toán rollback, y hệt khi Payment gọi thẳng như trước.
 */
public record PaymentConfirmedEvent(
        UUID transactionId,
        String relatedType,
        UUID relatedId
) {
}

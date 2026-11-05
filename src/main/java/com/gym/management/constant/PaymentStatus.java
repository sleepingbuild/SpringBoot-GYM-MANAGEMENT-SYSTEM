package com.gym.management.constant;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * Các giá trị hợp lệ của {@code payment_transactions.status}.
 *
 * REFUNDED: giao dịch POS đã SUCCESS nhưng đơn bị huỷ nên đã hoàn tiền
 * ({@code PaymentService#refundPayment}). Báo cáo doanh thu chỉ cộng SUCCESS nên tự loại giao dịch REFUNDED.
 */
public final class PaymentStatus {
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";

    private PaymentStatus() {
    }
}

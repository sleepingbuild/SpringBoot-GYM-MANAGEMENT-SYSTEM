package com.gym.management.constant;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * Giá trị hợp lệ của {@code payment_transactions.payment_method} ở v1.0
 * (chưa tích hợp cổng thanh toán ngoài — xem REQUIREMENTS.md mục 2 & ARCHITECTURE.md mục 5).
 */
public final class PaymentMethodType {
    public static final String CASH = "CASH";
    public static final String LOCAL_CONFIRM = "LOCAL_CONFIRM";

    private PaymentMethodType() {
    }
}

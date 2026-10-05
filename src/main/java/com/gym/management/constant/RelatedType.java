package com.gym.management.constant;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * Giá trị hợp lệ của {@code payment_transactions.related_type} — xác định
 * relatedId trỏ tới bảng nào (member_packages hay pos_orders).
 */
public final class RelatedType {
    public static final String MEMBERSHIP = "MEMBERSHIP";
    public static final String POS = "POS"; // dùng bởi Agent 5 (POS module, M4)

    private RelatedType() {
    }
}

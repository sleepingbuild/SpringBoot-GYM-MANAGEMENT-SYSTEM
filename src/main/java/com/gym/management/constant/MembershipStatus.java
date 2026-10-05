package com.gym.management.constant;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * Các giá trị hợp lệ của {@code member_packages.status} — dùng String thay vì JPA enum
 * để khớp quy ước "VARCHAR, không CHECK constraint cứng" trong DATABASE_SCHEMA.md
 * (dễ mở rộng giá trị sau này mà không cần migration đổi kiểu cột).
 *
 * State machine đầy đủ — xem REQUIREMENTS.md mục 2:
 * PENDING → ACTIVE → EXPIRED (lazy-check)
 *              │
 *   Nâng cấp: ACTIVE cũ → CANCELLED (ngay lập tức) + tạo bản ghi mới PENDING
 *   Hạ cấp:   ACTIVE cũ giữ nguyên      + tạo bản ghi mới SCHEDULED → ACTIVE (lazy-check, tới ngày)
 */
public final class MembershipStatus {
    public static final String PENDING = "PENDING";
    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String SCHEDULED = "SCHEDULED";
    public static final String CANCELLED = "CANCELLED";

    private MembershipStatus() {
    }
}

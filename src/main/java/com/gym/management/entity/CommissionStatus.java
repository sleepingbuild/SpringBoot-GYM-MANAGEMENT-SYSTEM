package com.gym.management.entity;

/**
 * Agent 5 - POS & Commission Module.
 * Trạng thái chi trả lương theo buổi của PT. PAID là thao tác thủ công của Super Admin
 * (POST /api/v1/commissions/{id}/mark-paid) — hệ thống không tự chuyển PENDING → PAID.
 */
public enum CommissionStatus {
    PENDING,
    PAID
}

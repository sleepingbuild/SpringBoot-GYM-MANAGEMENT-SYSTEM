package com.gym.management.entity;

/**
 * Enum tham chiếu type-safe cho tên role, dùng trong @PreAuthorize và logic RBAC.
 * Phải khớp chính xác với dữ liệu seed trong bảng `roles` (V1__init_schema.sql).
 */
public enum RoleName {
    SUPER_ADMIN,
    RECEPTIONIST,
    TRAINER,
    SALES,
    MEMBER
}

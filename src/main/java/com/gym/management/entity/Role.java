package com.gym.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Role cố định của hệ thống (Agent 1 - SHARED FILE).
 * Giá trị chuẩn: SUPER_ADMIN, RECEPTIONIST, TRAINER, SALES, MEMBER.
 * Xem RoleName enum để tham chiếu type-safe trong code (@PreAuthorize).
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;
}

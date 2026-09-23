package com.gym.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Agent 1 - Module Hồ sơ cá nhân.
 * Áp dụng cho Member và Trainer (tự xem/sửa tuổi, cân nặng, chiều cao, mục tiêu, avatar).
 * Validate tuổi ≥ 18 so với User.createdAt (ngày đăng ký) — xem REQUIREMENTS.md mục 1.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column
    private Integer age;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(length = 255)
    private String goal;

    @Column(name = "avatar_url")
    private String avatarUrl;
}

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
import java.time.LocalDate;

/**
 * Agent 1 - Module Hồ sơ cá nhân.
 * Áp dụng cho Member và Trainer (tự xem/sửa ngày sinh, cân nặng, chiều cao, mục tiêu, avatar).
 * Validate từ 18 tuổi trở lên tính đến ngày đăng ký (User.createdAt) ở tầng service,
 * vì ràng buộc liên quan hai bảng nên không đặt CHECK trong DB — xem REQUIREMENTS.md mục 1.
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

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 5, scale = 2)
    private BigDecimal heightCm;

    @Column(length = 255)
    private String goal;

    @Column(name = "avatar_url")
    private String avatarUrl;
}

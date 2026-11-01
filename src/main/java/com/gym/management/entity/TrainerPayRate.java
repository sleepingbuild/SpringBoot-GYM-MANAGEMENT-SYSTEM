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
 * Agent 5 - POS & Commission Module.
 * Mức lương mỗi buổi dạy của 1 PT — 1-1 với User (role TRAINER), do Super Admin cấu hình.
 * Thay cho cột "commission_rate" được nhắc trong TASK_ASSIGNMENT.md gốc nhưng không có
 * chỗ lưu trong schema cũ (xem Javadoc User.java + docs/CHANGE_PT_ATTENDANCE_AND_PAY.md).
 */
@Entity
@Table(name = "trainer_pay_rates")
@Getter
@Setter
@NoArgsConstructor
public class TrainerPayRate extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false, unique = true)
    private User trainer;

    @Column(name = "amount_per_session", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPerSession;
}

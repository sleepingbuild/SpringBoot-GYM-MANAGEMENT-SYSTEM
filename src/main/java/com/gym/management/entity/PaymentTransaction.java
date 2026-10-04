package com.gym.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * Log MỌI giao dịch thanh toán trong hệ thống — Membership (chính module này) VÀ
 * POS (Agent 5, xem ISSUES.md 2.8 / 5.2) — thông qua đúng 1 method dùng chung
 * {@link com.gym.management.service.PaymentService#confirmPayment}, không viết
 * riêng service xác nhận thanh toán cho từng module.
 *
 * relatedType/relatedId dùng kiểu "polymorphic thủ công" (không FK cứng) vì relatedId
 * có thể trỏ tới member_packages HOẶC pos_orders (2 bảng khác nhau) — đúng theo
 * DATABASE_SCHEMA.md.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "related_type", nullable = false, length = 20)
    private String relatedType; // MEMBERSHIP, POS

    @Column(name = "related_id", nullable = false)
    private UUID relatedId; // id của member_packages hoặc pos_orders

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod; // CASH, LOCAL_CONFIRM

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, SUCCESS, FAILED

    @Column(name = "transaction_code", nullable = false, unique = true, length = 100)
    private String transactionCode;
}

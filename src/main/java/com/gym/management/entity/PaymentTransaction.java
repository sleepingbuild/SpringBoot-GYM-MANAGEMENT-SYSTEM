package com.gym.management.entity;

import com.gym.management.constant.RelatedType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
 * Từ V7 (CH1): mỗi giao dịch thuộc ĐÚNG MỘT trong hai loại, lưu bằng 2 khoá ngoại thật thay cho cặp
 * related_type/related_id không có khoá ngoại: {@code memberPackageId} (member_packages) hoặc
 * {@code posOrderId} (pos_orders). Hai cột là UUID thô, không map quan hệ JPA, để không phụ thuộc cứng
 * vào entity của module khác. Code ngoài entity vẫn dùng cặp relatedType/relatedId (hằng số
 * {@link RelatedType}) qua {@link #setRelated}, {@link #getRelatedType}, {@link #getRelatedId}.
 *
 * {@code user} là NGƯỜI MUA (hội viên); NULL với đơn POS của khách vãng lai. Nhân viên thu tiền không
 * lưu ở đây (POS lưu ở pos_orders.staff_id).
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // nullable: khách vãng lai của POS
    private User user;

    @Column(name = "member_package_id")
    private UUID memberPackageId;

    @Column(name = "pos_order_id")
    private UUID posOrderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod; // CASH, LOCAL_CONFIRM

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, SUCCESS, FAILED

    @Column(name = "transaction_code", nullable = false, unique = true, length = 100)
    private String transactionCode;

    /** {@link RelatedType#MEMBERSHIP} nếu thuộc gói tập, {@link RelatedType#POS} nếu thuộc đơn POS, null nếu chưa gán. */
    @Transient
    public String getRelatedType() {
        if (memberPackageId != null) {
            return RelatedType.MEMBERSHIP;
        }
        return posOrderId != null ? RelatedType.POS : null;
    }

    /** id của member_packages hoặc pos_orders tuỳ {@link #getRelatedType()}. */
    @Transient
    public UUID getRelatedId() {
        return memberPackageId != null ? memberPackageId : posOrderId;
    }

    /** Gán loại và id liên quan; luôn xoá cột của loại còn lại để thoả ràng buộc "đúng một trong hai". */
    public void setRelated(String relatedType, UUID relatedId) {
        if (RelatedType.MEMBERSHIP.equals(relatedType)) {
            this.memberPackageId = relatedId;
            this.posOrderId = null;
        } else if (RelatedType.POS.equals(relatedType)) {
            this.posOrderId = relatedId;
            this.memberPackageId = null;
        } else {
            throw new IllegalArgumentException("relatedType không hợp lệ: " + relatedType);
        }
    }
}

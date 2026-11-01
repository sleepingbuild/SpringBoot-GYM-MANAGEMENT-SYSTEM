package com.gym.management.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agent 5 - POS & Commission Module.
 * Đơn bán hàng tại quầy. Thanh toán ghi qua {@code payment_transactions} dùng chung với
 * Membership (Agent 2) — xem Javadoc {@link com.gym.management.service.PaymentService} —
 * nên entity này KHÔNG có cột liên kết tới payment_transactions; chiều liên kết là
 * payment_transactions.related_id → pos_orders.id (RelatedType.POS).
 *
 * branch_id giữ dạng UUID thô (không map ManyToOne sang Branch) — cùng cách Booking.branchId
 * làm, để giảm phụ thuộc cứng vào entity Branch của Agent 1.
 */
@Entity
@Table(name = "pos_orders")
@Getter
@Setter
@NoArgsConstructor
public class PosOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private User member; // null = khách vãng lai

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod; // CASH, LOCAL_CONFIRM

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PosOrderStatus status = PosOrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt asc")
    private List<PosOrderItem> items = new ArrayList<>();

    /**
     * Tổng tiền đơn — KHÔNG lưu cột (CH5, docs/DB_DESIGN_REVIEW.md): suy ra từ quantity x unit_price
     * của các dòng; số tiền đã thu thực tế nằm ở payment_transactions.amount.
     */
    public BigDecimal getTotalAmount() {
        return items.stream().map(PosOrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(PosOrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}

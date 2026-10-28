package com.gym.management.entity;

import com.gym.management.service.event.BookingSettledEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Agent 5 - POS & Commission Module.
 * Lương theo buổi của PT — nguồn DUY NHẤT là {@link BookingSettledEvent} do Agent 3 publish
 * (xem {@code service.listener.BookingSettledCommissionListener}). Không còn hoa hồng Sales
 * (đã bỏ theo docs/CHANGE_PT_ATTENDANCE_AND_PAY.md mục 1.6).
 *
 * bookingId giữ dạng UUID thô (không ManyToOne sang Booking của Agent 3) — cùng lý do với
 * Booking.branchId: giảm phụ thuộc biên dịch cứng giữa 2 module, FK vẫn được ràng buộc ở
 * tầng DB (V5__pos_commission.sql). UNIQUE để 1 buổi tối đa 1 bản ghi, chống tính trùng khi
 * listener có thể nhận event này hơn 1 lần.
 */
@Entity
@Table(name = "commissions")
@Getter
@Setter
@NoArgsConstructor
public class Commission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingSettledEvent.Outcome outcome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommissionStatus status = CommissionStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}

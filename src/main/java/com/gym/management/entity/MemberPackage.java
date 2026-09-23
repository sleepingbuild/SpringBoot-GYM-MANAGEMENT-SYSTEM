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

import java.time.LocalDate;

/**
 * Agent 2 - Membership & Package Module.
 * Bản TỐI THIỂU (start_date/end_date/remaining_sessions/status) để BookingService
 * (Agent 3) áp được rule "giới hạn buổi/tuần" và trừ buổi khi hoàn thành. Agent 2
 * mở rộng thêm nghiệp vụ nâng/hạ cấp đầy đủ sau (xem REQUIREMENTS.md mục 2).
 */
@Entity
@Table(name = "member_packages")
@Getter
@Setter
@NoArgsConstructor
public class MemberPackage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private Package gymPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    /**
     * PENDING (chờ thanh toán) → ACTIVE (start_date = lúc thanh toán xong) → EXPIRED (lazy-check).
     * SCHEDULED dành cho hạ cấp (chờ gói cũ hết hạn) → tự chuyển ACTIVE đúng ngày.
     * Xem REQUIREMENTS.md mục 2 — "getCurrentMembership" định nghĩa gói hiện tại = status ACTIVE.
     */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "remaining_sessions")
    private Integer remainingSessions;
}

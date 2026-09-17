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
 * Bản TỐI THIỂU (start_date/end_date/remaining_sessions/status) để CheckInService
 * (Agent 3) chạy được luồng 6 bước. Agent 2 mở rộng thêm freeze/upgrade sau.
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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "remaining_sessions")
    private Integer remainingSessions;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, FROZEN, PENDING

    @Column(name = "total_frozen_days", nullable = false)
    private int totalFrozenDays = 0;
}

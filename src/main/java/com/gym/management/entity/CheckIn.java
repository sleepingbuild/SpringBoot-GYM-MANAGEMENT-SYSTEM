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

import java.time.Instant;

/**
 * Agent 3 - Check-in Engine & Access Control.
 */
@Entity
@Table(name = "check_ins")
@Getter
@Setter
@NoArgsConstructor
public class CheckIn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_package_id")
    private MemberPackage memberPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "checkin_time", nullable = false)
    private Instant checkinTime = Instant.now();

    @Column(nullable = false, length = 20)
    private String method; // QR, CARD, FACE

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, DENIED_EXPIRED, DENIED_TIME, DENIED_NO_SESSION, DENIED_ANTI_PASSBACK, DENIED_FACE_NOT_RECOGNIZED

    @Column(name = "device_id", length = 100)
    private String deviceId;
}

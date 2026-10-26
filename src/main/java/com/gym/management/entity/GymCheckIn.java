package com.gym.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lượt vào phòng tập của hội viên KHÔNG gắn với buổi PT (CH2 / V8).
 * Lượt có booking phù hợp chỉ cập nhật `bookings`, không tạo dòng ở đây.
 */
@Entity
@Table(
        name = "gym_check_ins",
        indexes = @Index(name = "idx_gym_check_ins_member_time", columnList = "member_id, check_in_time")
)
@Getter
@Setter
@NoArgsConstructor
public class GymCheckIn extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    // Kiosk hiện chưa biết chi nhánh nên để null.
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(nullable = false, length = 20)
    private String method = "FACE"; // FACE / MANUAL
}

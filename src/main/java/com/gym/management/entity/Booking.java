package com.gym.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Buổi đặt lịch tập với PT. Điểm danh buổi tập dùng trực tiếp check_in_time/check_out_time
 * ở đây (không có bảng check_ins riêng) — xem DATABASE_SCHEMA.md.
 *
 * check_in_time / check_out_time / check_in_method / check_out_method được Agent 4
 * (Face Attendance) ghi vào THÔNG QUA BookingService (xem markFaceScan trong BookingService),
 * không truy cập Repository trực tiếp — giữ đúng nguyên tắc kiến trúc ở ARCHITECTURE.md.
 */
@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_trainer_date", columnList = "trainer_id, booking_date"),
                @Index(name = "idx_booking_member_date", columnList = "member_id, booking_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    /**
     * branch_id giữ dạng UUID thô (không map quan hệ ManyToOne sang Branch) để giảm phụ thuộc
     * cứng vào entity Branch của Agent 1 — chỉ cần đúng UUID hợp lệ khi query/lọc theo chi nhánh.
     */
    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_in_method", length = 20)
    private String checkInMethod;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "check_out_method", length = 20)
    private String checkOutMethod;

    @Column(name = "notes", length = 500)
    private String notes;
}

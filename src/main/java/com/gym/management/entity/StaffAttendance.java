package com.gym.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "staff_attendances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "date"}),
        indexes = @Index(name = "idx_staff_attendances_date", columnList = "date")
)
@Getter
@Setter
@NoArgsConstructor
public class StaffAttendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(nullable = false, length = 20)
    private String method = "MANUAL"; // MANUAL / FACE

    @Column(length = 255)
    private String notes;
}

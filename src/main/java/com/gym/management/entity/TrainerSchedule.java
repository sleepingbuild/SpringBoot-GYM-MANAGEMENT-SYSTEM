package com.gym.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Ca làm việc của PT theo NGÀY CỤ THỂ (work_date), KHÔNG theo DayOfWeek lặp lại.
 * Xem REQUIREMENTS.md mục 3 — lý do đổi model từ thực tế dự án .NET song song:
 * dùng DayOfWeek từng gây lỗi coi 2 tuần khác nhau cùng Thứ 2 là trùng lịch.
 *
 * GIẢ ĐỊNH (cần Agent 1 xác nhận — xem AGENT3_README.md mục 1):
 * - BaseEntity cung cấp sẵn: id (UUID, PK), createdAt, updatedAt (auditing), version (optimistic lock).
 * - User (Agent 1) có khoá chính UUID, dùng trực tiếp làm quan hệ ManyToOne.
 */
@Entity
@Table(
        name = "trainer_schedules",
        indexes = {
                @Index(name = "idx_trainer_schedule_trainer_date", columnList = "trainer_id, work_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer;

    /** Ngày cụ thể của ca làm việc — nguồn chân lý duy nhất, KHÔNG dùng day-of-week. */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** false = ca "Tạm nghỉ" (soft-disable, không xoá để giữ lịch sử). */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}

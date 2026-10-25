package com.gym.management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerScheduleRequest {

    /**
     * Bỏ trống khi PT tự tạo ca cho chính mình (lấy từ @CurrentUser).
     * Bắt buộc khi Admin tạo hộ cho 1 PT khác.
     */
    private UUID trainerId;

    @NotNull(message = "work_date là bắt buộc")
    private LocalDate workDate;

    @NotNull(message = "start_time là bắt buộc")
    private LocalTime startTime;

    @NotNull(message = "end_time là bắt buộc")
    private LocalTime endTime;

    private Boolean active;
}

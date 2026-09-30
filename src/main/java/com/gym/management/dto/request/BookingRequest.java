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
public class BookingRequest {

    /** Bỏ trống khi Member tự đặt (lấy từ @CurrentUser). Bắt buộc khi Lễ tân đặt hộ. */
    private UUID memberId;

    @NotNull(message = "trainerId là bắt buộc")
    private UUID trainerId;

    private UUID branchId;

    @NotNull(message = "bookingDate là bắt buộc")
    private LocalDate bookingDate;

    @NotNull(message = "startTime là bắt buộc")
    private LocalTime startTime;

    @NotNull(message = "endTime là bắt buộc")
    private LocalTime endTime;

    private String notes;
}

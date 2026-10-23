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

/**
 * Sửa lịch — KHÔNG cho đổi member_id của 1 booking đã tồn tại (đổi PT/ngày/giờ thì được).
 * Toàn bộ rule của tạo mới được áp lại y hệt (REQUIREMENTS.md mục 3, Issue 3.2).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingUpdateRequest {

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

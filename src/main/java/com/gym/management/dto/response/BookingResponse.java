package com.gym.management.dto.response;

import com.gym.management.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private UUID id;
    private UUID memberId;
    private String memberName;
    private UUID trainerId;
    private String trainerName;
    private UUID branchId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private LocalDateTime checkInTime;
    private String checkInMethod;
    private LocalDateTime checkOutTime;
    private String checkOutMethod;
    private String notes;
}

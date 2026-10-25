package com.gym.management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class StaffAttendanceResponse {
    private UUID id;
    private UUID staffId;
    private String staffName;
    private LocalDate date;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String method;
    private String computedStatus; // ON_TIME / LATE / LEFT_EARLY / LATE_AND_LEFT_EARLY / ABSENT
    private String notes;
}

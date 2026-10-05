package com.gym.management.dto.response;

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
public class TrainerScheduleResponse {
    private UUID id;
    private UUID trainerId;
    private String trainerName;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean active;
}

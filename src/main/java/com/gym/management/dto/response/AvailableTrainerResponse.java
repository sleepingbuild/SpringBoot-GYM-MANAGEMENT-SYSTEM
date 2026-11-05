package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Mot PT con slot trong trong ngay duoc hoi (xem TrainerScheduleService#listAvailableTrainers). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableTrainerResponse {
    private UUID trainerId;
    private String trainerName;
    private int availableSlotCount;
}
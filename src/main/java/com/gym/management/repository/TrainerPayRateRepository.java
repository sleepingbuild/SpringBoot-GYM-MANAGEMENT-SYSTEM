package com.gym.management.repository;

import com.gym.management.entity.TrainerPayRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainerPayRateRepository extends JpaRepository<TrainerPayRate, UUID> {

    Optional<TrainerPayRate> findByTrainerId(UUID trainerId);
}

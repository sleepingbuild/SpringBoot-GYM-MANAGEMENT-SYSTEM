package com.gym.management.repository;

import com.gym.management.entity.GymCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GymCheckInRepository extends JpaRepository<GymCheckIn, UUID> {

    Optional<GymCheckIn> findFirstByMemberIdOrderByCheckInTimeDesc(UUID memberId);

    List<GymCheckIn> findByMemberIdOrderByCheckInTimeDesc(UUID memberId);
}

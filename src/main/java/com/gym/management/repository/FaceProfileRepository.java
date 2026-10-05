package com.gym.management.repository;

import com.gym.management.entity.FaceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaceProfileRepository extends JpaRepository<FaceProfile, UUID> {
    Optional<FaceProfile> findByUserId(UUID userId);
    List<FaceProfile> findAll(); // dùng cho luồng Kiosk 1:N
    void deleteByUserId(UUID userId);
}

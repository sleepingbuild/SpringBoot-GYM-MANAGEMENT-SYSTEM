package com.gym.management.repository;

import com.gym.management.entity.FaceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FaceProfileRepository extends JpaRepository<FaceProfile, UUID> {

    Optional<FaceProfile> findByUserId(UUID userId);

    /** Mock: so khớp byte-for-byte theo SHA-256 hash của ảnh — xem MockFaceMatcher. */
    Optional<FaceProfile> findByImageHash(String imageHash);
}

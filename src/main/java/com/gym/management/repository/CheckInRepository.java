package com.gym.management.repository;

import com.gym.management.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    /** Lấy lần check-in SUCCESS gần nhất của user — dùng cho kiểm tra anti-passback. */
    Optional<CheckIn> findFirstByUserIdAndStatusOrderByCheckinTimeDesc(UUID userId, String status);

    List<CheckIn> findByUserIdOrderByCheckinTimeDesc(UUID userId);

    List<CheckIn> findByBranchIdOrderByCheckinTimeDesc(UUID branchId);
}

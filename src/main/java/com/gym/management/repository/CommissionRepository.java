package com.gym.management.repository;

import com.gym.management.entity.Commission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CommissionRepository extends JpaRepository<Commission, UUID> {

    boolean existsByBookingId(UUID bookingId);

    Page<Commission> findByStaffId(UUID staffId, Pageable pageable);

    @Query("select c from Commission c where (:staffId is null or c.staff.id = :staffId) "
            + "and (:from is null or c.createdAt >= :from) and (:to is null or c.createdAt < :to)")
    Page<Commission> search(@Param("staffId") UUID staffId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             Pageable pageable);

    List<Commission> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}

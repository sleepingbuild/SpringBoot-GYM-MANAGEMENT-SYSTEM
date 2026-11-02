package com.gym.management.repository;

import com.gym.management.entity.PosOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PosOrderRepository extends JpaRepository<PosOrder, UUID> {

    @Query("select o from PosOrder o where (:branchId is null or o.branchId = :branchId) "
            + "and (:from is null or o.createdAt >= :from) and (:to is null or o.createdAt < :to)")
    Page<PosOrder> search(@Param("branchId") UUID branchId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           Pageable pageable);
}

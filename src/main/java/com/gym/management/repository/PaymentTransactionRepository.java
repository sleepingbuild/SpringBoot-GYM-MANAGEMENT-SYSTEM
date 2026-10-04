package com.gym.management.repository;

import com.gym.management.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<PaymentTransaction> findByUserId(UUID userId, Pageable pageable);

    Optional<PaymentTransaction> findFirstByRelatedTypeAndRelatedIdOrderByCreatedAtDesc(String relatedType, UUID relatedId);

    /**
     * Thay cho GET /payments?branchId=... (hoãn — payment_transactions không có branch_id,
     * xem AGENT2_README.md). Lễ tân/Admin tra cứu theo khoảng ngày thay vì theo chi nhánh.
     */
    Page<PaymentTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}

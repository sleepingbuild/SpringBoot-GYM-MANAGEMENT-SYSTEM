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

    /** Giao dịch mới nhất của 1 gói tập (thay cho findFirstByRelatedTypeAndRelatedId..., bỏ ở V7). */
    Optional<PaymentTransaction> findFirstByMemberPackageIdOrderByCreatedAtDesc(UUID memberPackageId);

    /** Giao dịch mới nhất của 1 đơn POS — để huỷ/hoàn tiền đơn POS tìm lại giao dịch của nó. */
    Optional<PaymentTransaction> findFirstByPosOrderIdOrderByCreatedAtDesc(UUID posOrderId);

    /**
     * Thay cho GET /payments?branchId=... (hoãn — payment_transactions không có branch_id,
     * xem AGENT2_README.md). Lễ tân/Admin tra cứu theo khoảng ngày thay vì theo chi nhánh.
     */
    Page<PaymentTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}

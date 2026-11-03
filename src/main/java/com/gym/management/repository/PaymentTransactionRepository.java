package com.gym.management.repository;

import com.gym.management.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "user")
    Page<PaymentTransaction> findByUserId(UUID userId, Pageable pageable);

    /** Giao dịch mới nhất của 1 gói tập (thay cho findFirstByRelatedTypeAndRelatedId..., bỏ ở V7). */
    Optional<PaymentTransaction> findFirstByMemberPackageIdOrderByCreatedAtDesc(UUID memberPackageId);

    /** Giao dịch mới nhất của 1 đơn POS — để huỷ/hoàn tiền đơn POS tìm lại giao dịch của nó. */
    Optional<PaymentTransaction> findFirstByPosOrderIdOrderByCreatedAtDesc(UUID posOrderId);

    /**
     * Thay cho GET /payments?branchId=... (hoãn — payment_transactions không có branch_id,
     * xem AGENT2_README.md). Lễ tân/Admin tra cứu theo khoảng ngày thay vì theo chi nhánh.
     *
     * Các biến thể lọc thêm theo trạng thái / người mua (H3) là truy vấn dẫn xuất riêng thay vì một truy vấn
     * có tham số tuỳ chọn: SQL Server + Hibernate bind tham số UUID null thành varbinary nên dễ lỗi chuyển kiểu.
     * EntityGraph nạp luôn user (LEFT JOIN, giữ cả giao dịch không có user) để lấy userFullName không bị N+1.
     */
    @EntityGraph(attributePaths = "user")
    Page<PaymentTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<PaymentTransaction> findByStatusAndCreatedAtBetween(String status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<PaymentTransaction> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<PaymentTransaction> findByUserIdAndStatusAndCreatedAtBetween(UUID userId, String status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<PaymentTransaction> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<PaymentTransaction> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);
}

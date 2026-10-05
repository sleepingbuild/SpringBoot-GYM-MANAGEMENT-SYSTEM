package com.gym.management.service;

import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.entity.PaymentTransaction;
import com.gym.management.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Agent 2 - Membership, Package & Payment Module.
 *
 * ⚠️ 1 NGUỒN CHÂN LÝ cho việc xác nhận thanh toán (xem ISSUES.md 2.8) — dùng chung
 * cho CẢ Membership (module này) LẪN POS (Agent 5, từ M4). Agent 5 KHÔNG tự viết
 * service xác nhận thanh toán riêng, chỉ gọi lại {@link #createPendingTransaction}
 * (nếu POS cần bước chờ xác nhận) hoặc {@link #confirmPayment} (transactionId đã có).
 */
public interface PaymentService {

    /**
     * Tạo 1 bản ghi payment_transactions ở trạng thái PENDING — dùng khi luồng nghiệp vụ
     * cần tách rời bước "phát sinh giao dịch" và "xác nhận đã thu tiền" (đúng luồng Membership:
     * đăng ký → PENDING → lễ tân xác nhận → ACTIVE).
     */
    PaymentTransactionResponse createPendingTransaction(User user, String relatedType, UUID relatedId, BigDecimal amount);

    /**
     * Xác nhận 1 giao dịch đã tồn tại (PENDING → SUCCESS). Idempotent: gọi lại khi đã SUCCESS
     * thì trả nguyên trạng, không lỗi. Nếu relatedType = MEMBERSHIP, tự động gọi
     * {@link MembershipService#activateFromPendingPayment} để kích hoạt gói tương ứng —
     * đây là điểm nối DUY NHẤT giữa Payment và Membership, không lặp lại logic activate ở nơi khác.
     */
    PaymentTransactionResponse confirmPayment(UUID transactionId, String paymentMethod);

    PaymentTransaction getEntityById(UUID transactionId);

    /** @deprecated dùng {@link #getMyPayments(UUID, org.springframework.data.domain.Pageable)}. */
    @Deprecated
    List<PaymentTransactionResponse> getMyPayments(UUID userId);

    com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getMyPayments(UUID userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Thay cho GET /payments?branchId=... (hoãn theo quyết định Phi — payment_transactions
     * không có branch_id). Lễ tân/Admin tra cứu theo khoảng ngày [from, to).
     */
    com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getAllPayments(
            java.time.LocalDate from, java.time.LocalDate to, org.springframework.data.domain.Pageable pageable);
}

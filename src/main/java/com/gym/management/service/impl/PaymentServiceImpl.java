package com.gym.management.service.impl;

import com.gym.management.constant.PaymentMethodType;
import com.gym.management.constant.PaymentStatus;
import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.entity.PaymentTransaction;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.PaymentTransactionRepository;
import com.gym.management.service.PaymentService;
import com.gym.management.service.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 2 - Membership, Package & Payment Module.
 *
 * Lưu ý về phụ thuộc: PaymentServiceImpl KHÔNG phụ thuộc MembershipService. Trước đây 2 bên inject
 * constructor lẫn nhau (Membership → Payment để tạo giao dịch PENDING lúc đăng ký/nâng/hạ cấp;
 * Payment → Membership để kích hoạt gói trong confirmPayment) nên ứng dụng không khởi động được —
 * Spring Boot cấm vòng phụ thuộc bean theo mặc định (nhận định cũ ở đây rằng "Spring xử lý được qua
 * proxy" là SAI, mvn verify với Mockito không bắt được lỗi này).
 *
 * Nay chỉ còn 1 chiều Membership → Payment. Khi xác nhận xong 1 giao dịch, Payment phát
 * {@link PaymentConfirmedEvent} (đồng bộ, chung transaction) và
 * {@link com.gym.management.service.listener.MembershipPaymentListener} kích hoạt gói. Module khác
 * (vd POS của Agent 5) lắng nghe cùng sự kiện này và lọc theo relatedType.
 * KHÔNG thêm lại phụ thuộc ngược Payment → Membership và KHÔNG bật spring.main.allow-circular-references.
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PaymentTransactionResponse createPendingTransaction(User user, String relatedType, UUID relatedId, BigDecimal amount) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setUser(user);
        tx.setRelatedType(relatedType);
        tx.setRelatedId(relatedId);
        tx.setAmount(amount);
        tx.setPaymentMethod(PaymentMethodType.CASH); // mặc định, có thể đổi lúc confirm
        tx.setStatus(PaymentStatus.PENDING);
        tx.setTransactionCode(generateTransactionCode());
        return toResponse(saveWithRetryOnCodeCollision(tx));
    }

    @Override
    @Transactional
    public PaymentTransactionResponse confirmPayment(UUID transactionId, String paymentMethod) {
        PaymentTransaction tx = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (PaymentStatus.SUCCESS.equals(tx.getStatus())) {
            return toResponse(tx); // idempotent — xác nhận lần 2 không lỗi, không phát lại sự kiện, không lặp side-effect
        }

        if (paymentMethod != null && !paymentMethod.isBlank()) {
            tx.setPaymentMethod(paymentMethod);
        }
        tx.setStatus(PaymentStatus.SUCCESS);
        tx = paymentTransactionRepository.save(tx);

        // Điểm nối DUY NHẤT giữa Payment và các module khác — Payment không gọi thẳng Membership (tránh vòng
        // phụ thuộc bean), chỉ phát sự kiện đồng bộ chung transaction:
        //  - MEMBERSHIP: MembershipPaymentListener kích hoạt gói (PENDING → ACTIVE).
        //  - POS: Agent 5 có thể lắng nghe cùng sự kiện để trừ kho ở module riêng; module này chỉ đảm bảo
        //    giao dịch được ghi log và đổi trạng thái đúng.
        eventPublisher.publishEvent(new PaymentConfirmedEvent(tx.getId(), tx.getRelatedType(), tx.getRelatedId()));

        return toResponse(tx);
    }

    @Override
    public PaymentTransaction getEntityById(UUID transactionId) {
        return paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Override
    public List<PaymentTransactionResponse> getMyPayments(UUID userId) {
        return paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getMyPayments(
            UUID userId, org.springframework.data.domain.Pageable pageable) {
        return com.gym.management.dto.response.PageResponse.from(
                paymentTransactionRepository.findByUserId(userId, pageable), this::toResponse);
    }

    @Override
    public com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getAllPayments(
            java.time.LocalDate from, java.time.LocalDate to, org.springframework.data.domain.Pageable pageable) {
        java.time.LocalDate effectiveFrom = from != null ? from : java.time.LocalDate.now();
        java.time.LocalDate effectiveTo = to != null ? to : java.time.LocalDate.now();
        java.time.LocalDateTime fromDateTime = effectiveFrom.atStartOfDay();
        java.time.LocalDateTime toDateTime = effectiveTo.plusDays(1).atStartOfDay(); // exclusive upper bound, gồm trọn ngày "to"
        return com.gym.management.dto.response.PageResponse.from(
                paymentTransactionRepository.findByCreatedAtBetween(fromDateTime, toDateTime, pageable), this::toResponse);
    }

    private PaymentTransaction saveWithRetryOnCodeCollision(PaymentTransaction tx) {
        try {
            return paymentTransactionRepository.save(tx);
        } catch (DataIntegrityViolationException ex) {
            // transaction_code trùng (xác suất cực thấp với UUID) — thử lại 1 lần với code mới.
            tx.setTransactionCode(generateTransactionCode());
            try {
                return paymentTransactionRepository.save(tx);
            } catch (DataIntegrityViolationException ex2) {
                throw new BusinessException(ErrorCode.PAYMENT_DUPLICATE_TRANSACTION);
            }
        }
    }

    private String generateTransactionCode() {
        return "TX-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentTransactionResponse toResponse(PaymentTransaction tx) {
        return PaymentTransactionResponse.builder()
                .id(tx.getId())
                .userId(tx.getUser().getId())
                .relatedType(tx.getRelatedType())
                .relatedId(tx.getRelatedId())
                .amount(tx.getAmount())
                .paymentMethod(tx.getPaymentMethod())
                .status(tx.getStatus())
                .transactionCode(tx.getTransactionCode())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

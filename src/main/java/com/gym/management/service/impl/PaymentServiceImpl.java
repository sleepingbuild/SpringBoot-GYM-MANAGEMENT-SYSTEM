package com.gym.management.service.impl;

import com.gym.management.constant.PaymentMethodType;
import com.gym.management.constant.PaymentStatus;
import com.gym.management.constant.RelatedType;
import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.entity.PaymentTransaction;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.PaymentTransactionRepository;
import com.gym.management.service.MembershipService;
import com.gym.management.service.PaymentService;
import lombok.RequiredArgsConstructor;
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
 * Lưu ý về phụ thuộc: PaymentServiceImpl PHỤ THUỘC MembershipService (1 chiều) để gọi
 * activateFromPendingPayment khi confirm 1 giao dịch MEMBERSHIP. Chiều ngược lại
 * (MembershipServiceImpl → PaymentService) cũng tồn tại (để tạo giao dịch PENDING lúc
 * đăng ký/nâng/hạ cấp) — do đó về mặt Spring bean đây LÀ một cặp phụ thuộc 2 chiều.
 * Không phát sinh circular-dependency lỗi vì cả 2 constructor chỉ cần INTERFACE của
 * nhau (Spring xử lý được qua proxy khi cả 2 đều là @Service cùng module), nhưng nếu
 * sau này refactor tách nhỏ hơn, cân nhắc dùng ApplicationEventPublisher để tách rời
 * hẳn 2 luồng thay vì gọi trực tiếp.
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final MembershipService membershipService;

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
            return toResponse(tx); // idempotent — xác nhận lần 2 không lỗi, không lặp side-effect
        }

        if (paymentMethod != null && !paymentMethod.isBlank()) {
            tx.setPaymentMethod(paymentMethod);
        }
        tx.setStatus(PaymentStatus.SUCCESS);
        tx = paymentTransactionRepository.save(tx);

        // Điểm nối DUY NHẤT giữa Payment và Membership — không lặp lại logic activate ở nơi khác.
        if (RelatedType.MEMBERSHIP.equals(tx.getRelatedType())) {
            membershipService.activateFromPendingPayment(tx.getRelatedId());
        }
        // RelatedType.POS: Agent 5 tự xử lý tiếp nghiệp vụ trừ kho ở module riêng, module này
        // chỉ đảm bảo giao dịch được ghi log và đổi trạng thái đúng.

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

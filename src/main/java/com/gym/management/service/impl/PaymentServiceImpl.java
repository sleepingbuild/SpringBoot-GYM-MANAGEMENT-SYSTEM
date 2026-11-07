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
import com.gym.management.service.PaymentService;
import com.gym.management.service.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Set;
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
    private final Clock clock;

    @Override
    @Transactional
    public PaymentTransactionResponse createPendingTransaction(User user, String relatedType, UUID relatedId, BigDecimal amount) {
        // relatedType/relatedId được ánh xạ sang 2 khoá ngoại member_package_id / pos_order_id (V7).
        if (relatedId == null || !(RelatedType.MEMBERSHIP.equals(relatedType) || RelatedType.POS.equals(relatedType))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        PaymentTransaction tx = new PaymentTransaction();
        tx.setUser(user); // null = khách vãng lai của POS (đơn không gắn hội viên)
        tx.setRelated(relatedType, relatedId);
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
    @Transactional
    public PaymentTransactionResponse refundPayment(UUID transactionId) {
        PaymentTransaction tx = paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        return refund(tx);
    }

    @Override
    @Transactional
    public PaymentTransactionResponse refundByRelated(String relatedType, UUID relatedId) {
        if (relatedId == null || !(RelatedType.MEMBERSHIP.equals(relatedType) || RelatedType.POS.equals(relatedType))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (RelatedType.MEMBERSHIP.equals(relatedType)) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_REFUNDABLE); // gói tập chưa hỗ trợ hoàn tiền
        }
        // POS: đơn không có giao dịch nào = không có gì để hoàn → PAYMENT_NOT_REFUNDABLE (theo quyết định của Agent 1)
        PaymentTransaction tx = paymentTransactionRepository.findFirstByPosOrderIdOrderByCreatedAtDesc(relatedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_REFUNDABLE));
        return refund(tx);
    }

    private PaymentTransactionResponse refund(PaymentTransaction tx) {
        if (PaymentStatus.REFUNDED.equals(tx.getStatus())) {
            return toResponse(tx); // idempotent — gọi lại không hoàn lần 2
        }
        // Chỉ hoàn giao dịch POS đã thu tiền. Gói tập chưa hỗ trợ hoàn tiền (gói có thể đã được dùng).
        if (!RelatedType.POS.equals(tx.getRelatedType()) || !PaymentStatus.SUCCESS.equals(tx.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        }
        tx.setStatus(PaymentStatus.REFUNDED);
        return toResponse(paymentTransactionRepository.save(tx));
    }

    @Override
    public PaymentTransaction getEntityById(UUID transactionId) {
        return paymentTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getMyPayments(UUID userId) {
        return paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getMyPayments(
            UUID userId, org.springframework.data.domain.Pageable pageable) {
        return com.gym.management.dto.response.PageResponse.from(
                paymentTransactionRepository.findByUserId(userId, pageable), this::toResponse);
    }

    private static final Set<String> VALID_STATUSES = Set.of(
            PaymentStatus.PENDING, PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.REFUNDED);

    @Override
    @Transactional(readOnly = true)
    public com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getAllPayments(
            java.time.LocalDate from, java.time.LocalDate to, String status, UUID userId,
            org.springframework.data.domain.Pageable pageable) {
        String st = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        if (st != null && !VALID_STATUSES.contains(st)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        // Không truyền ngày và có bộ lọc status/userId → lấy mọi ngày (tìm giao dịch chờ xác nhận cũ).
        // Không truyền ngày và không bộ lọc nào → hôm nay, như trước đây.
        boolean ranged = from != null || to != null || (st == null && userId == null);
        org.springframework.data.domain.Page<PaymentTransaction> page;
        if (ranged) {
            java.time.LocalDate effectiveFrom = from != null ? from : java.time.LocalDate.now(clock);
            java.time.LocalDate effectiveTo = to != null ? to : java.time.LocalDate.now(clock);
            java.time.LocalDateTime fromDateTime = effectiveFrom.atStartOfDay();
            java.time.LocalDateTime toDateTime = effectiveTo.plusDays(1).atStartOfDay(); // exclusive upper bound, gồm trọn ngày "to"
            if (st == null && userId == null) {
                page = paymentTransactionRepository.findByCreatedAtBetween(fromDateTime, toDateTime, pageable);
            } else if (userId == null) {
                page = paymentTransactionRepository.findByStatusAndCreatedAtBetween(st, fromDateTime, toDateTime, pageable);
            } else if (st == null) {
                page = paymentTransactionRepository.findByUserIdAndCreatedAtBetween(userId, fromDateTime, toDateTime, pageable);
            } else {
                page = paymentTransactionRepository.findByUserIdAndStatusAndCreatedAtBetween(userId, st, fromDateTime, toDateTime, pageable);
            }
        } else if (userId == null) {
            page = paymentTransactionRepository.findByStatus(st, pageable);
        } else if (st == null) {
            page = paymentTransactionRepository.findByUserId(userId, pageable);
        } else {
            page = paymentTransactionRepository.findByUserIdAndStatus(userId, st, pageable);
        }
        return com.gym.management.dto.response.PageResponse.from(page, this::toResponse);
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
                .userId(tx.getUser() != null ? tx.getUser().getId() : null) // null: khách vãng lai POS
                .userFullName(tx.getUser() != null ? tx.getUser().getFullName() : null)
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

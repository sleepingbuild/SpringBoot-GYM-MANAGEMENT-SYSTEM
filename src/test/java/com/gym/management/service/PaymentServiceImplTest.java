package com.gym.management.service;

import com.gym.management.constant.PaymentStatus;
import com.gym.management.constant.RelatedType;
import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.entity.PaymentTransaction;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.PaymentTransactionRepository;
import com.gym.management.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cho PaymentService — đặc biệt {@link com.gym.management.service.PaymentService#confirmPayment}
 * vì đây là điểm nối DUY NHẤT giữa Payment và Membership (xem Javadoc PaymentServiceImpl).
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private MembershipService membershipService;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentTransactionRepository, membershipService);
    }

    private PaymentTransaction pendingMembershipTransaction(UUID memberPackageId) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(UUID.randomUUID());
        User user = new User();
        user.setId(UUID.randomUUID());
        tx.setUser(user);
        tx.setRelatedType(RelatedType.MEMBERSHIP);
        tx.setRelatedId(memberPackageId);
        tx.setAmount(new BigDecimal("500000"));
        tx.setPaymentMethod("CASH");
        tx.setStatus(PaymentStatus.PENDING);
        tx.setTransactionCode("TX-TEST-0001");
        return tx;
    }

    @Test
    void confirmPayment_shouldActivateMembership_whenRelatedTypeIsMembership() {
        UUID memberPackageId = UUID.randomUUID();
        PaymentTransaction tx = pendingMembershipTransaction(memberPackageId);

        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipService.activateFromPendingPayment(memberPackageId))
                .thenReturn(MemberPackageResponse.builder().id(memberPackageId).build());

        var response = paymentService.confirmPayment(tx.getId(), "CASH");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        // Điểm nối bắt buộc: confirm 1 giao dịch MEMBERSHIP PHẢI kích hoạt đúng member_package tương ứng
        verify(membershipService, times(1)).activateFromPendingPayment(memberPackageId);
    }

    @Test
    void confirmPayment_shouldBeIdempotent_whenAlreadySuccess() {
        UUID memberPackageId = UUID.randomUUID();
        PaymentTransaction tx = pendingMembershipTransaction(memberPackageId);
        tx.setStatus(PaymentStatus.SUCCESS); // đã confirm từ trước

        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        var response = paymentService.confirmPayment(tx.getId(), "CASH");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        // Gọi lại lần 2 KHÔNG được kích hoạt membership thêm lần nữa (tránh side-effect lặp)
        verify(membershipService, never()).activateFromPendingPayment(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void confirmPayment_shouldNotTouchMembership_whenRelatedTypeIsPos() {
        PaymentTransaction tx = pendingMembershipTransaction(UUID.randomUUID());
        tx.setRelatedType(RelatedType.POS); // giao dịch POS (Agent 5), không phải Membership

        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.confirmPayment(tx.getId(), null);

        verify(membershipService, never()).activateFromPendingPayment(any());
    }

    @Test
    void confirmPayment_shouldThrow_whenTransactionNotFound() {
        UUID missingId = UUID.randomUUID();
        when(paymentTransactionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(missingId, "CASH"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }
}

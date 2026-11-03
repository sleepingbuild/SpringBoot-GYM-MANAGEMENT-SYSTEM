package com.gym.management.service;

import com.gym.management.constant.PaymentStatus;
import com.gym.management.constant.RelatedType;
import com.gym.management.entity.PaymentTransaction;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.PaymentTransactionRepository;
import com.gym.management.service.event.PaymentConfirmedEvent;
import com.gym.management.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test cho PaymentService — đặc biệt {@link com.gym.management.service.PaymentService#confirmPayment}
 * vì đây là điểm nối DUY NHẤT giữa Payment và các module khác: Payment KHÔNG gọi thẳng MembershipService
 * (tránh vòng phụ thuộc bean, xem Javadoc PaymentServiceImpl) mà phát {@link PaymentConfirmedEvent}.
 * Việc listener kích hoạt đúng gói được test ở MembershipPaymentListenerTest và MembershipPaymentWiringTest.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    // 17:00 ngày 27/10/2026 giờ Việt Nam — dùng để kiểm tra khoảng ngày mặc định theo bean Clock
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentTransactionRepository, eventPublisher, CLOCK);
    }

    private PaymentTransaction pendingMembershipTransaction(UUID memberPackageId) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(UUID.randomUUID());
        User user = new User();
        user.setId(UUID.randomUUID());
        tx.setUser(user);
        tx.setRelated(RelatedType.MEMBERSHIP, memberPackageId);
        tx.setAmount(new BigDecimal("500000"));
        tx.setPaymentMethod("CASH");
        tx.setStatus(PaymentStatus.PENDING);
        tx.setTransactionCode("TX-TEST-0001");
        return tx;
    }

    @Test
    void confirmPayment_shouldPublishEventAfterSaving_whenRelatedTypeIsMembership() {
        UUID memberPackageId = UUID.randomUUID();
        PaymentTransaction tx = pendingMembershipTransaction(memberPackageId);

        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.confirmPayment(tx.getId(), "CASH");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        // Điểm nối bắt buộc: confirm 1 giao dịch MEMBERSHIP PHẢI phát sự kiện mang đúng member_package cần kích hoạt,
        // và phải phát SAU khi đã lưu trạng thái SUCCESS (listener chạy đồng bộ, chung transaction).
        InOrder order = inOrder(paymentTransactionRepository, eventPublisher);
        order.verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        order.verify(eventPublisher).publishEvent(
                new PaymentConfirmedEvent(tx.getId(), RelatedType.MEMBERSHIP, memberPackageId));
    }

    @Test
    void confirmPayment_shouldBeIdempotent_whenAlreadySuccess() {
        UUID memberPackageId = UUID.randomUUID();
        PaymentTransaction tx = pendingMembershipTransaction(memberPackageId);
        tx.setStatus(PaymentStatus.SUCCESS); // đã confirm từ trước

        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        var response = paymentService.confirmPayment(tx.getId(), "CASH");

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        // Gọi lại lần 2 KHÔNG được phát sự kiện lần nữa (nếu không gói sẽ bị kích hoạt lặp / side-effect lặp)
        verify(eventPublisher, never()).publishEvent(any(PaymentConfirmedEvent.class));
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void confirmPayment_shouldPublishEventWithPosType_soOtherModulesCanReact() {
        PaymentTransaction tx = pendingMembershipTransaction(UUID.randomUUID());
        UUID posOrderId = UUID.randomUUID();
        tx.setRelated(RelatedType.POS, posOrderId); // giao dịch POS (Agent 5), không phải Membership

        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.confirmPayment(tx.getId(), null);

        // Payment không phân biệt loại giao dịch: luôn phát sự kiện, MembershipPaymentListener tự bỏ qua POS
        // (xem MembershipPaymentListenerTest), còn Agent 5 có thể lắng nghe cùng sự kiện để trừ kho.
        verify(eventPublisher).publishEvent(new PaymentConfirmedEvent(tx.getId(), RelatedType.POS, posOrderId));
    }

    @Test
    void confirmPayment_shouldThrow_whenTransactionNotFound() {
        UUID missingId = UUID.randomUUID();
        when(paymentTransactionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(missingId, "CASH"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void createPendingTransaction_membership_shouldStoreMemberPackageId() {
        UUID memberPackageId = UUID.randomUUID();
        User buyer = new User();
        buyer.setId(UUID.randomUUID());
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPendingTransaction(
                buyer, RelatedType.MEMBERSHIP, memberPackageId, new BigDecimal("500000"));

        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(saved.capture());
        assertThat(saved.getValue().getMemberPackageId()).isEqualTo(memberPackageId);
        assertThat(saved.getValue().getPosOrderId()).isNull();
        assertThat(response.getRelatedType()).isEqualTo(RelatedType.MEMBERSHIP);
        assertThat(response.getRelatedId()).isEqualTo(memberPackageId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void createPendingTransaction_posWalkIn_shouldAllowNullUser() {
        UUID posOrderId = UUID.randomUUID();
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // khách vãng lai: đơn POS không gắn hội viên nên user = null
        var response = paymentService.createPendingTransaction(
                null, RelatedType.POS, posOrderId, new BigDecimal("45000"));

        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isNull();
        assertThat(saved.getValue().getPosOrderId()).isEqualTo(posOrderId);
        assertThat(saved.getValue().getMemberPackageId()).isNull();
        assertThat(response.getUserId()).isNull();
        assertThat(response.getRelatedType()).isEqualTo(RelatedType.POS);
        assertThat(response.getRelatedId()).isEqualTo(posOrderId);
    }

    @Test
    void createPendingTransaction_shouldRejectUnknownRelatedTypeOrMissingId() {
        assertThatThrownBy(() -> paymentService.createPendingTransaction(
                null, "OTHER", UUID.randomUUID(), BigDecimal.TEN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> paymentService.createPendingTransaction(
                null, RelatedType.POS, null, BigDecimal.TEN))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(paymentTransactionRepository);
    }

    @Test
    void getAllPayments_shouldDefaultToTodayOfInjectedClock_whenNoRange() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate today = LocalDate.of(2026, 10, 27);
        when(paymentTransactionRepository.findByCreatedAtBetween(any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<PaymentTransaction>(List.of(), pageable, 0));

        paymentService.getAllPayments(null, null, pageable);

        verify(paymentTransactionRepository).findByCreatedAtBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), pageable);
    }

    @Test
    void getAllPayments_shouldUseGivenRange_whenProvided() {
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentTransactionRepository.findByCreatedAtBetween(any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<PaymentTransaction>(List.of(), pageable, 0));

        paymentService.getAllPayments(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), pageable);

        verify(paymentTransactionRepository).findByCreatedAtBetween(
                LocalDate.of(2026, 1, 1).atStartOfDay(), LocalDate.of(2026, 2, 1).atStartOfDay(), pageable);
    }

    // ===== refundPayment (hoàn tiền đơn POS) =====

    private PaymentTransaction posTransaction(String status) {
        PaymentTransaction tx = pendingMembershipTransaction(UUID.randomUUID());
        tx.setRelated(RelatedType.POS, UUID.randomUUID());
        tx.setStatus(status);
        return tx;
    }

    @Test
    void refundPayment_shouldMarkPosSuccessTransactionAsRefunded() {
        PaymentTransaction tx = posTransaction(PaymentStatus.SUCCESS);
        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.refundPayment(tx.getId());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentTransactionRepository).save(tx);
    }

    @Test
    void refundPayment_shouldBeIdempotent_whenAlreadyRefunded() {
        PaymentTransaction tx = posTransaction(PaymentStatus.REFUNDED);
        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        var response = paymentService.refundPayment(tx.getId());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void refundPayment_shouldRejectMembershipTransaction() {
        PaymentTransaction tx = pendingMembershipTransaction(UUID.randomUUID());
        tx.setStatus(PaymentStatus.SUCCESS); // giao dịch gói tập chưa hỗ trợ hoàn tiền
        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> paymentService.refundPayment(tx.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void refundPayment_shouldRejectPosTransactionThatIsNotSuccess() {
        PaymentTransaction tx = posTransaction(PaymentStatus.PENDING); // chưa thu tiền thì không có gì để hoàn
        when(paymentTransactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> paymentService.refundPayment(tx.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void refundPayment_shouldThrowNotFound_whenTransactionMissing() {
        UUID missingId = UUID.randomUUID();
        when(paymentTransactionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundPayment(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    void refundByRelated_pos_shouldFindLatestTransactionOfTheOrderAndRefundIt() {
        PaymentTransaction tx = posTransaction(PaymentStatus.SUCCESS);
        UUID posOrderId = tx.getPosOrderId();
        when(paymentTransactionRepository.findFirstByPosOrderIdOrderByCreatedAtDesc(posOrderId))
                .thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.refundByRelated(RelatedType.POS, posOrderId);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refundByRelated_pos_shouldBeIdempotent_whenAlreadyRefunded() {
        PaymentTransaction tx = posTransaction(PaymentStatus.REFUNDED);
        UUID posOrderId = tx.getPosOrderId();
        when(paymentTransactionRepository.findFirstByPosOrderIdOrderByCreatedAtDesc(posOrderId))
                .thenReturn(Optional.of(tx));

        var response = paymentService.refundByRelated(RelatedType.POS, posOrderId); // huỷ đơn thử lại: không lỗi, không hoàn thêm

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void refundByRelated_pos_shouldBeNotRefundable_whenOrderHasNoTransaction() {
        UUID posOrderId = UUID.randomUUID();
        when(paymentTransactionRepository.findFirstByPosOrderIdOrderByCreatedAtDesc(posOrderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundByRelated(RelatedType.POS, posOrderId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_REFUNDABLE);
    }

    @Test
    void refundByRelated_pos_shouldBeNotRefundable_whenLatestTransactionIsNotSuccess() {
        PaymentTransaction tx = posTransaction(PaymentStatus.PENDING);
        UUID posOrderId = tx.getPosOrderId();
        when(paymentTransactionRepository.findFirstByPosOrderIdOrderByCreatedAtDesc(posOrderId))
                .thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> paymentService.refundByRelated(RelatedType.POS, posOrderId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    void refundByRelated_membership_shouldBeNotRefundable_withoutTouchingRepository() {
        assertThatThrownBy(() -> paymentService.refundByRelated(RelatedType.MEMBERSHIP, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        verifyNoInteractions(paymentTransactionRepository);
    }

    @Test
    void refundByRelated_shouldRejectUnknownTypeOrNullId() {
        assertThatThrownBy(() -> paymentService.refundByRelated("OTHER", UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> paymentService.refundByRelated(RelatedType.POS, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(paymentTransactionRepository);
    }

    // ===== getAllPayments: lọc theo trạng thái / người mua (H3) =====

    private static final Pageable PAGE = PageRequest.of(0, 20);

    private PageImpl<PaymentTransaction> emptyPage() {
        return new PageImpl<PaymentTransaction>(List.of(), PAGE, 0);
    }

    @Test
    void getAllPayments_statusOnlyWithoutDates_shouldSearchAllDays() {
        when(paymentTransactionRepository.findByStatus("PENDING", PAGE)).thenReturn(emptyPage());

        paymentService.getAllPayments(null, null, " pending ", null, PAGE); // chuẩn hoá khoảng trắng, chữ thường

        verify(paymentTransactionRepository).findByStatus("PENDING", PAGE);
    }

    @Test
    void getAllPayments_userOnlyWithoutDates_shouldSearchAllDays() {
        UUID userId = UUID.randomUUID();
        when(paymentTransactionRepository.findByUserId(userId, PAGE)).thenReturn(emptyPage());

        paymentService.getAllPayments(null, null, null, userId, PAGE);

        verify(paymentTransactionRepository).findByUserId(userId, PAGE);
    }

    @Test
    void getAllPayments_userAndStatusWithoutDates_shouldSearchAllDays() {
        UUID userId = UUID.randomUUID();
        when(paymentTransactionRepository.findByUserIdAndStatus(userId, "SUCCESS", PAGE)).thenReturn(emptyPage());

        paymentService.getAllPayments(null, null, "SUCCESS", userId, PAGE);

        verify(paymentTransactionRepository).findByUserIdAndStatus(userId, "SUCCESS", PAGE);
    }

    @Test
    void getAllPayments_statusWithDates_shouldFilterByBoth() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        when(paymentTransactionRepository.findByStatusAndCreatedAtBetween(
                "PENDING", from.atStartOfDay(), to.plusDays(1).atStartOfDay(), PAGE)).thenReturn(emptyPage());

        paymentService.getAllPayments(from, to, "PENDING", null, PAGE);

        verify(paymentTransactionRepository).findByStatusAndCreatedAtBetween(
                "PENDING", from.atStartOfDay(), to.plusDays(1).atStartOfDay(), PAGE);
    }

    @Test
    void getAllPayments_userAndStatusWithDates_shouldFilterByAll() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        when(paymentTransactionRepository.findByUserIdAndStatusAndCreatedAtBetween(
                userId, "REFUNDED", from.atStartOfDay(), to.plusDays(1).atStartOfDay(), PAGE)).thenReturn(emptyPage());

        paymentService.getAllPayments(from, to, "REFUNDED", userId, PAGE);

        verify(paymentTransactionRepository).findByUserIdAndStatusAndCreatedAtBetween(
                userId, "REFUNDED", from.atStartOfDay(), to.plusDays(1).atStartOfDay(), PAGE);
    }

    @Test
    void getAllPayments_userWithDates_shouldFilterByUserAndDates() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 10, 1);
        when(paymentTransactionRepository.findByUserIdAndCreatedAtBetween(
                eq(userId), eq(from.atStartOfDay()), any(), eq(PAGE))).thenReturn(emptyPage());

        paymentService.getAllPayments(from, null, null, userId, PAGE); // thiếu vế "to" thì mặc định hôm nay theo Clock

        verify(paymentTransactionRepository).findByUserIdAndCreatedAtBetween(
                userId, from.atStartOfDay(), LocalDate.of(2026, 10, 28).atStartOfDay(), PAGE);
    }

    @Test
    void getAllPayments_noFilterNoDates_shouldStillDefaultToToday() {
        LocalDate today = LocalDate.of(2026, 10, 27); // CLOCK
        when(paymentTransactionRepository.findByCreatedAtBetween(any(), any(), eq(PAGE))).thenReturn(emptyPage());

        paymentService.getAllPayments(null, null, null, null, PAGE);

        verify(paymentTransactionRepository).findByCreatedAtBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), PAGE);
    }

    @Test
    void getAllPayments_shouldRejectUnknownStatus() {
        assertThatThrownBy(() -> paymentService.getAllPayments(null, null, "BOGUS", null, PAGE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(paymentTransactionRepository);
    }

    @Test
    void getAllPayments_shouldTreatBlankStatusAsNoFilter() {
        LocalDate today = LocalDate.of(2026, 10, 27);
        when(paymentTransactionRepository.findByCreatedAtBetween(any(), any(), eq(PAGE))).thenReturn(emptyPage());

        paymentService.getAllPayments(null, null, "   ", null, PAGE);

        verify(paymentTransactionRepository).findByCreatedAtBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay(), PAGE);
    }

    @Test
    void createPendingTransaction_shouldExposeBuyerFullName_andNullForWalkIn() {
        User buyer = new User();
        buyer.setId(UUID.randomUUID());
        buyer.setFullName("Nguyen Van A");
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var withBuyer = paymentService.createPendingTransaction(
                buyer, RelatedType.MEMBERSHIP, UUID.randomUUID(), new BigDecimal("500000"));
        var walkIn = paymentService.createPendingTransaction(
                null, RelatedType.POS, UUID.randomUUID(), new BigDecimal("45000"));

        assertThat(withBuyer.getUserFullName()).isEqualTo("Nguyen Van A");
        assertThat(walkIn.getUserFullName()).isNull();
    }
}

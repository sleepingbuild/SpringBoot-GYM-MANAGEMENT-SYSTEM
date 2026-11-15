package com.gym.management.service.impl;

import com.gym.management.constant.PaymentMethodType;
import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.dto.response.PackagePurchaseResponse;
import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.service.MembershipService;
import com.gym.management.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberSelfPurchaseServiceImplTest {

    @Mock
    private MembershipService membershipService;
    @Mock
    private PaymentService paymentService;
    @InjectMocks
    private MemberSelfPurchaseServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID packageId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID txId = UUID.randomUUID();

    private PackagePurchaseResponse registeredPending(PaymentTransactionResponse pendingTx) {
        when(pendingTx.getId()).thenReturn(txId);
        return PackagePurchaseResponse.builder()
                .memberPackage(MemberPackageResponse.builder().status("PENDING").build())
                .paymentTransaction(pendingTx)
                .build();
    }

    @Test
    void purchase_registersThenConfirmsAndReturnsActivePackage() {
        PaymentTransactionResponse pendingTx = mock(PaymentTransactionResponse.class);
        PaymentTransactionResponse confirmedTx = mock(PaymentTransactionResponse.class);
        MemberPackageResponse active = MemberPackageResponse.builder().status("ACTIVE").remainingSessions(12).build();

        PackagePurchaseResponse registered = registeredPending(pendingTx);
        when(membershipService.registerNewPackage(userId, packageId, branchId)).thenReturn(registered);
        when(paymentService.confirmPayment(txId, PaymentMethodType.LOCAL_CONFIRM)).thenReturn(confirmedTx);
        when(membershipService.getCurrentMembershipResponse(userId)).thenReturn(active);

        PackagePurchaseResponse result = service.purchase(userId, packageId, branchId);

        assertThat(result.getMemberPackage()).isSameAs(active);
        assertThat(result.getPaymentTransaction()).isSameAs(confirmedTx);
        InOrder order = inOrder(membershipService, paymentService);
        order.verify(membershipService).registerNewPackage(userId, packageId, branchId);
        order.verify(paymentService).confirmPayment(txId, PaymentMethodType.LOCAL_CONFIRM);
        order.verify(membershipService).getCurrentMembershipResponse(userId);
    }

    @Test
    void purchase_whenRegisterFails_doesNotConfirmPayment() {
        when(membershipService.registerNewPackage(userId, packageId, null))
                .thenThrow(new BusinessException(ErrorCode.MEMBERSHIP_PENDING_EXISTS));

        assertThatThrownBy(() -> service.purchase(userId, packageId, null))
                .isInstanceOf(BusinessException.class);

        verify(paymentService, never()).confirmPayment(any(), any());
    }

    @Test
    void purchase_whenConfirmFails_propagatesErrorAndDoesNotBuildResponse() {
        PaymentTransactionResponse pendingTx = mock(PaymentTransactionResponse.class);
        PackagePurchaseResponse registered = registeredPending(pendingTx);
        when(membershipService.registerNewPackage(userId, packageId, branchId)).thenReturn(registered);
        when(paymentService.confirmPayment(txId, PaymentMethodType.LOCAL_CONFIRM))
                .thenThrow(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        assertThatThrownBy(() -> service.purchase(userId, packageId, branchId))
                .isInstanceOf(BusinessException.class);

        verify(membershipService, never()).getCurrentMembershipResponse(any());
    }
}

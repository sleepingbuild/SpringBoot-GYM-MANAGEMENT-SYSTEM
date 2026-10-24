package com.gym.management.service.listener;

import com.gym.management.constant.RelatedType;
import com.gym.management.service.MembershipService;
import com.gym.management.service.event.PaymentConfirmedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MembershipPaymentListenerTest {

    @Mock
    private MembershipService membershipService;

    @InjectMocks
    private MembershipPaymentListener listener;

    @Test
    @DisplayName("Giao dịch MEMBERSHIP được xác nhận: kích hoạt đúng member_package")
    void membershipPayment_activatesMemberPackage() {
        UUID memberPackageId = UUID.randomUUID();

        listener.onPaymentConfirmed(
                new PaymentConfirmedEvent(UUID.randomUUID(), RelatedType.MEMBERSHIP, memberPackageId));

        verify(membershipService).activateFromPendingPayment(memberPackageId);
    }

    @Test
    @DisplayName("Giao dịch không thuộc Membership (vd POS): bỏ qua, không đụng member_package")
    void nonMembershipPayment_isIgnored() {
        listener.onPaymentConfirmed(
                new PaymentConfirmedEvent(UUID.randomUUID(), "POS", UUID.randomUUID()));

        verifyNoInteractions(membershipService);
    }
}

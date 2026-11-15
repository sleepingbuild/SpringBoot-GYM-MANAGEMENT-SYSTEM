package com.gym.management.service.impl;

import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.service.MembershipService;
import com.gym.management.service.OpenBookingCountPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyMembershipOverviewServiceImplTest {

    @Mock
    private MembershipService membershipService;
    @Mock
    private OpenBookingCountPort openBookingCountPort;
    @InjectMocks
    private MyMembershipOverviewServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    private MemberPackageResponse packageWith(Integer remaining) {
        return MemberPackageResponse.builder().status("ACTIVE").remainingSessions(remaining).build();
    }

    @Test
    void remainingMinusHeld_isBookable() {
        when(membershipService.getCurrentMembershipResponse(userId)).thenReturn(packageWith(12));
        when(openBookingCountPort.countOpenBookings(userId)).thenReturn(3);

        MemberPackageResponse result = service.getMyCurrentMembership(userId);

        assertThat(result.getRemainingSessions()).isEqualTo(12);
        assertThat(result.getHeldSessions()).isEqualTo(3);
        assertThat(result.getBookableSessions()).isEqualTo(9);
    }

    @Test
    void heldMoreThanRemaining_bookableNeverNegative() {
        when(membershipService.getCurrentMembershipResponse(userId)).thenReturn(packageWith(2));
        when(openBookingCountPort.countOpenBookings(userId)).thenReturn(5);

        MemberPackageResponse result = service.getMyCurrentMembership(userId);

        assertThat(result.getHeldSessions()).isEqualTo(5);
        assertThat(result.getBookableSessions()).isZero();
    }

    @Test
    void unlimitedPackage_leavesBothNullAndDoesNotCallBooking() {
        when(membershipService.getCurrentMembershipResponse(userId)).thenReturn(packageWith(null));

        MemberPackageResponse result = service.getMyCurrentMembership(userId);

        assertThat(result.getHeldSessions()).isNull();
        assertThat(result.getBookableSessions()).isNull();
        verifyNoInteractions(openBookingCountPort);
    }

    @Test
    void noActiveMembership_propagatesErrorAndDoesNotCallBooking() {
        when(membershipService.getCurrentMembershipResponse(userId))
                .thenThrow(new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        assertThatThrownBy(() -> service.getMyCurrentMembership(userId)).isInstanceOf(BusinessException.class);

        verifyNoInteractions(openBookingCountPort);
    }
}

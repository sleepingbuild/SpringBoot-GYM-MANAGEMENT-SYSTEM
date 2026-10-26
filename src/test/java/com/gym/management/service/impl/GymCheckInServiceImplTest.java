package com.gym.management.service.impl;

import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.entity.GymCheckIn;
import com.gym.management.entity.MemberPackage;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.GymCheckInRepository;
import com.gym.management.service.GymCheckInService;
import com.gym.management.service.MembershipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
class GymCheckInServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 18, 0);

    @Mock private GymCheckInRepository gymCheckInRepository;
    @Mock private MembershipService membershipService;
    @InjectMocks private GymCheckInServiceImpl service;

    private final UUID memberId = UUID.randomUUID();

    private GymCheckIn checkIn(LocalDateTime checkInTime) {
        GymCheckIn c = new GymCheckIn();
        c.setMemberId(memberId);
        c.setCheckInTime(checkInTime);
        return c;
    }

    private void givenActiveMembership() {
        when(membershipService.getCurrentMembership(memberId)).thenReturn(Optional.of(mock(MemberPackage.class)));
    }

    // ---------------------------------------------------------------- lượt 1: check-in

    @Test
    void firstScan_withActiveMembership_createsCheckIn() {
        when(gymCheckInRepository.findFirstByMemberIdOrderByCheckInTimeDesc(memberId)).thenReturn(Optional.empty());
        givenActiveMembership();
        when(gymCheckInRepository.save(any(GymCheckIn.class))).thenAnswer(i -> i.getArgument(0));

        GymCheckInService.ScanResult result = service.recordFaceScan(memberId, NOW);

        assertThat(result.action()).isEqualTo(GymCheckInService.ACTION_CHECK_IN);
        ArgumentCaptor<GymCheckIn> captor = ArgumentCaptor.forClass(GymCheckIn.class);
        verify(gymCheckInRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(memberId);
        assertThat(captor.getValue().getCheckInTime()).isEqualTo(NOW);
        assertThat(captor.getValue().getCheckOutTime()).isNull();
        assertThat(captor.getValue().getMethod()).isEqualTo("FACE");
        assertThat(captor.getValue().getBranchId()).isNull();
    }

    // ---------------------------------------------------------------- gói không hợp lệ

    @Test
    void firstScan_memberHasNoPackageAtAll_throwsMembershipNotFound_andSavesNothing() {
        when(gymCheckInRepository.findFirstByMemberIdOrderByCheckInTimeDesc(memberId)).thenReturn(Optional.empty());
        when(membershipService.getCurrentMembership(memberId)).thenReturn(Optional.empty());
        when(membershipService.getHistory(memberId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.recordFaceScan(memberId, NOW))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBERSHIP_NOT_FOUND);

        verify(gymCheckInRepository, never()).save(any());
    }

    @Test
    void firstScan_memberPackageExpired_throwsMembershipExpired_andSavesNothing() {
        when(gymCheckInRepository.findFirstByMemberIdOrderByCheckInTimeDesc(memberId)).thenReturn(Optional.empty());
        when(membershipService.getCurrentMembership(memberId)).thenReturn(Optional.empty());
        when(membershipService.getHistory(memberId)).thenReturn(List.of(mock(MemberPackageResponse.class)));

        assertThatThrownBy(() -> service.recordFaceScan(memberId, NOW))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBERSHIP_EXPIRED);

        verify(gymCheckInRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- lượt 2: check-out

    @Test
    void openCheckInSameDay_secondScanIsCheckOut_withoutMembershipCheck() {
        GymCheckIn open = checkIn(NOW.minusHours(1));
        when(gymCheckInRepository.findFirstByMemberIdOrderByCheckInTimeDesc(memberId)).thenReturn(Optional.of(open));

        GymCheckInService.ScanResult result = service.recordFaceScan(memberId, NOW);

        assertThat(result.action()).isEqualTo(GymCheckInService.ACTION_CHECK_OUT);
        assertThat(open.getCheckOutTime()).isEqualTo(NOW);
        verify(gymCheckInRepository).save(open);
        verifyNoInteractions(membershipService);
    }

    // ---------------------------------------------------------------- lượt 3: check-in mới

    @Test
    void closedCheckIn_thirdScanCreatesNewCheckIn() {
        GymCheckIn closed = checkIn(NOW.minusHours(3));
        closed.setCheckOutTime(NOW.minusHours(2));
        when(gymCheckInRepository.findFirstByMemberIdOrderByCheckInTimeDesc(memberId)).thenReturn(Optional.of(closed));
        givenActiveMembership();
        when(gymCheckInRepository.save(any(GymCheckIn.class))).thenAnswer(i -> i.getArgument(0));

        GymCheckInService.ScanResult result = service.recordFaceScan(memberId, NOW);

        assertThat(result.action()).isEqualTo(GymCheckInService.ACTION_CHECK_IN);
        verify(gymCheckInRepository).save(argThat(c -> c != closed && c.getCheckInTime().equals(NOW)));
        assertThat(closed.getCheckOutTime()).isEqualTo(NOW.minusHours(2)); // dòng cũ không bị đổi
    }

    // ---------------------------------------------------------------- dòng mở từ hôm trước

    @Test
    void openCheckInFromPreviousDay_isNotClosed_newCheckInCreated() {
        GymCheckIn stale = checkIn(NOW.minusDays(1));
        when(gymCheckInRepository.findFirstByMemberIdOrderByCheckInTimeDesc(memberId)).thenReturn(Optional.of(stale));
        givenActiveMembership();
        when(gymCheckInRepository.save(any(GymCheckIn.class))).thenAnswer(i -> i.getArgument(0));

        GymCheckInService.ScanResult result = service.recordFaceScan(memberId, NOW);

        assertThat(result.action()).isEqualTo(GymCheckInService.ACTION_CHECK_IN);
        assertThat(stale.getCheckOutTime()).isNull(); // không tự đóng dòng cũ
        verify(gymCheckInRepository, times(1)).save(any(GymCheckIn.class));
        verify(gymCheckInRepository, never()).save(stale);
    }
}

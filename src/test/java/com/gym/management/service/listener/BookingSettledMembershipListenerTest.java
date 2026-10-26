package com.gym.management.service.listener;

import com.gym.management.service.MembershipService;
import com.gym.management.service.MembershipService.ConsumeResult;
import com.gym.management.service.event.BookingSettledEvent;
import com.gym.management.service.event.BookingSettledEvent.Outcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSettledMembershipListenerTest {

    @Mock
    private MembershipService membershipService;

    @InjectMocks
    private BookingSettledMembershipListener listener;

    private BookingSettledEvent event(Outcome outcome, boolean fullDuration, UUID trainerId, UUID memberId) {
        return new BookingSettledEvent(UUID.randomUUID(), trainerId, memberId, outcome, fullDuration,
                LocalDateTime.now());
    }

    @ParameterizedTest
    @EnumSource(Outcome.class)
    @DisplayName("Cả 3 kết cục (COMPLETED, MEMBER_NO_SHOW, LATE_CANCEL) đều trừ đúng 1 buổi của hội viên")
    void everySettledOutcome_consumesOneSessionOfTheMember(Outcome outcome) {
        UUID memberId = UUID.randomUUID();
        when(membershipService.tryConsumeSession(memberId)).thenReturn(ConsumeResult.CONSUMED);

        listener.onBookingSettled(event(outcome, true, UUID.randomUUID(), memberId));

        verify(membershipService, times(1)).tryConsumeSession(memberId);
    }

    @Test
    @DisplayName("fullDuration = false chỉ ảnh hưởng lương PT (Agent 5), hội viên vẫn bị trừ buổi")
    void notFullDuration_stillConsumesMemberSession() {
        UUID memberId = UUID.randomUUID();
        when(membershipService.tryConsumeSession(memberId)).thenReturn(ConsumeResult.CONSUMED);

        listener.onBookingSettled(event(Outcome.COMPLETED, false, UUID.randomUUID(), memberId));

        verify(membershipService, times(1)).tryConsumeSession(memberId);
    }

    @Test
    @DisplayName("Trừ buổi của HỘI VIÊN, không bao giờ trừ theo id của PT")
    void consumesMemberNotTrainer() {
        UUID trainerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(membershipService.tryConsumeSession(memberId)).thenReturn(ConsumeResult.CONSUMED);

        listener.onBookingSettled(event(Outcome.COMPLETED, true, trainerId, memberId));

        verify(membershipService).tryConsumeSession(memberId);
        verify(membershipService, never()).tryConsumeSession(trainerId);
    }

    @Test
    @DisplayName("Gói không giới hạn buổi: không có gì để trừ, không lỗi")
    void unlimitedPackage_isNotAnError() {
        UUID memberId = UUID.randomUUID();
        when(membershipService.tryConsumeSession(memberId)).thenReturn(ConsumeResult.UNLIMITED);

        assertThatCode(() -> listener.onBookingSettled(
                event(Outcome.COMPLETED, true, UUID.randomUUID(), memberId)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Hội viên không còn gói ACTIVE: không ném lỗi để không chặn việc chốt buổi / trả công PT")
    void noActiveMembership_doesNotThrow() {
        UUID memberId = UUID.randomUUID();
        when(membershipService.tryConsumeSession(memberId)).thenReturn(ConsumeResult.NO_ACTIVE_MEMBERSHIP);

        assertThatCode(() -> listener.onBookingSettled(
                event(Outcome.MEMBER_NO_SHOW, true, UUID.randomUUID(), memberId)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Gói đã hết buổi: không ném lỗi để không chặn việc chốt buổi / trả công PT")
    void noSessionLeft_doesNotThrow() {
        UUID memberId = UUID.randomUUID();
        when(membershipService.tryConsumeSession(memberId)).thenReturn(ConsumeResult.NO_SESSION_LEFT);

        assertThatCode(() -> listener.onBookingSettled(
                event(Outcome.LATE_CANCEL, true, UUID.randomUUID(), memberId)))
                .doesNotThrowAnyException();
    }
}

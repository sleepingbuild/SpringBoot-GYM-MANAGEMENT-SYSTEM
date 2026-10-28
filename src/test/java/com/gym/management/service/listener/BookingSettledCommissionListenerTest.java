package com.gym.management.service.listener;

import com.gym.management.service.CommissionService;
import com.gym.management.service.event.BookingSettledEvent;
import com.gym.management.service.event.BookingSettledEvent.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingSettledCommissionListenerTest {

    @Mock private CommissionService commissionService;
    @InjectMocks private BookingSettledCommissionListener listener;

    private BookingSettledEvent event(Outcome outcome, boolean fullDuration) {
        return new BookingSettledEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                outcome, fullDuration, LocalDateTime.now());
    }

    @Test
    void threeOutcomesWithFullDuration_recordPay() {
        for (Outcome outcome : Outcome.values()) {
            BookingSettledEvent e = event(outcome, true);
            listener.onBookingSettled(e);
            verify(commissionService).recordSessionPay(e.trainerId(), e.bookingId(), outcome);
        }
    }

    @Test
    void notFullDuration_isIgnored() {
        listener.onBookingSettled(event(Outcome.COMPLETED, false));

        verify(commissionService, never()).recordSessionPay(any(), any(), any());
    }

    @Test
    void serviceFailure_neverPropagates_soBookingTransactionIsSafe() {
        doThrow(new RuntimeException("boom")).when(commissionService).recordSessionPay(any(), any(), any());

        assertThatCode(() -> listener.onBookingSettled(event(Outcome.COMPLETED, true))).doesNotThrowAnyException();
    }
}

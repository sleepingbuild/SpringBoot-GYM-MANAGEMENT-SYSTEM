package com.gym.management.service.impl;

import com.gym.management.dto.response.BookingResponse;
import com.gym.management.dto.response.CheckInHistoryItem;
import com.gym.management.entity.GymCheckIn;
import com.gym.management.repository.GymCheckInRepository;
import com.gym.management.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInHistoryServiceImplTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 10, 25, 18, 0);

    @Mock private GymCheckInRepository gymCheckInRepository;
    @Mock private BookingService bookingService;
    @InjectMocks private CheckInHistoryServiceImpl service;

    private final UUID memberId = UUID.randomUUID();

    private GymCheckIn gymVisit(LocalDateTime in, LocalDateTime out) {
        GymCheckIn c = new GymCheckIn();
        c.setMemberId(memberId);
        c.setCheckInTime(in);
        c.setCheckOutTime(out);
        return c;
    }

    private BookingResponse booking(LocalDateTime in, LocalDateTime out) {
        return BookingResponse.builder()
                .id(UUID.randomUUID())
                .memberId(memberId)
                .trainerName("PT An")
                .checkInTime(in)
                .checkInMethod("FACE")
                .checkOutTime(out)
                .build();
    }

    @Test
    void mergesBothSources_newestFirst_withTypes() {
        when(gymCheckInRepository.findByMemberIdOrderByCheckInTimeDesc(memberId))
                .thenReturn(List.of(gymVisit(T.minusDays(1), T.minusDays(1).plusHours(1))));
        when(bookingService.findMyBookings(eq(memberId), anyString()))
                .thenReturn(List.of(booking(T, null), booking(T.minusDays(3), T.minusDays(3).plusHours(1))));

        List<CheckInHistoryItem> result = service.findMyHistory(memberId);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(CheckInHistoryItem::getCheckInTime)
                .containsExactly(T, T.minusDays(1), T.minusDays(3));
        assertThat(result).extracting(CheckInHistoryItem::getType)
                .containsExactly(CheckInHistoryItem.TYPE_PT_SESSION,
                        CheckInHistoryItem.TYPE_GYM_VISIT,
                        CheckInHistoryItem.TYPE_PT_SESSION);
        assertThat(result.get(0).getTrainerName()).isEqualTo("PT An");
        assertThat(result.get(0).getCheckOutTime()).isNull();
        assertThat(result.get(1).getTrainerName()).isNull();
    }

    @Test
    void bookingsWithoutMemberCheckIn_areExcluded() {
        when(gymCheckInRepository.findByMemberIdOrderByCheckInTimeDesc(memberId)).thenReturn(List.of());
        when(bookingService.findMyBookings(eq(memberId), anyString()))
                .thenReturn(List.of(booking(null, null)));

        assertThat(service.findMyHistory(memberId)).isEmpty();
    }

    @Test
    void noData_returnsEmptyList() {
        when(gymCheckInRepository.findByMemberIdOrderByCheckInTimeDesc(memberId)).thenReturn(List.of());
        when(bookingService.findMyBookings(eq(memberId), anyString())).thenReturn(List.of());

        assertThat(service.findMyHistory(memberId)).isEmpty();
    }
}

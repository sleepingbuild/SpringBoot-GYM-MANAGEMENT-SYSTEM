package com.gym.management.service.impl;

import com.gym.management.dto.response.ExpiringMemberResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.ExpiringMemberRepository;
import com.gym.management.repository.ExpiringMemberRepository.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpiringMembershipServiceImplTest {

    @Mock ExpiringMemberRepository repo;
    ExpiringMembershipServiceImpl service;

    @BeforeEach
    void setUp() {
        // 2026-11-02 10:00 giờ Việt Nam
        Clock clock = Clock.fixed(Instant.parse("2026-11-02T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new ExpiringMembershipServiceImpl(repo, clock);
    }

    @Test
    void queriesFromTodayToTodayPlusDays() {
        when(repo.findExpiringBetween(any(), any())).thenReturn(List.of());

        service.findExpiringSoon(14);

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(repo).findExpiringBetween(from.capture(), to.capture());
        assertEquals(LocalDate.of(2026, 11, 2), from.getValue());
        assertEquals(LocalDate.of(2026, 11, 16), to.getValue());
    }

    @Test
    void computesDaysLeftAndKeepsRowOrder() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(repo.findExpiringBetween(any(), any())).thenReturn(List.of(
                new Row(a, "An", "0901234567", "an@test.local", "Goi 1 thang", LocalDate.of(2026, 11, 2), 3),
                new Row(b, "Binh", null, "binh@test.local", "Goi 3 thang", LocalDate.of(2026, 11, 9), null)));

        List<ExpiringMemberResponse> res = service.findExpiringSoon(7);

        assertEquals(2, res.size());
        assertEquals(a, res.get(0).userId());
        assertEquals(0, res.get(0).daysLeft());
        assertEquals(3, res.get(0).remainingSessions());
        assertEquals(b, res.get(1).userId());
        assertEquals(7, res.get(1).daysLeft());
        assertNull(res.get(1).phone());
        assertNull(res.get(1).remainingSessions());
    }

    @Test
    void returnsEmptyListWhenNobodyExpires() {
        when(repo.findExpiringBetween(any(), any())).thenReturn(List.of());
        assertTrue(service.findExpiringSoon(7).isEmpty());
    }

    @Test
    void acceptsBoundaryDays() {
        when(repo.findExpiringBetween(any(), any())).thenReturn(List.of());
        service.findExpiringSoon(1);
        service.findExpiringSoon(90);
    }

    @Test
    void rejectsDaysOutOfRange() {
        for (int bad : new int[] {0, -5, 91}) {
            BusinessException ex = assertThrows(BusinessException.class, () -> service.findExpiringSoon(bad));
            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }
        verify(repo, never()).findExpiringBetween(any(), any());
    }
}

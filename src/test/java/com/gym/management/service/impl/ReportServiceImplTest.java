package com.gym.management.service.impl;

import com.gym.management.dto.response.CheckinTrafficResponse;
import com.gym.management.dto.response.RenewalRateResponse;
import com.gym.management.dto.response.RevenueReportResponse;
import com.gym.management.dto.response.TopPackagesResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.ReportRepository;
import com.gym.management.repository.ReportRepository.RenewalCounts;
import com.gym.management.repository.ReportRepository.RevenueRow;
import com.gym.management.repository.ReportRepository.TopPackageRow;
import com.gym.management.repository.ReportRepository.TrafficRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {

    @Mock ReportRepository repo;
    @InjectMocks ReportServiceImpl service;

    @BeforeEach
    void fixClock() {
        // 2026-10-16 10:00 giờ Việt Nam
        service.clock = Clock.fixed(Instant.parse("2026-10-16T03:00:00Z"), ReportServiceImpl.VN_ZONE);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    // ---------- khoảng ngày ----------

    @Test
    void defaultRangeIsFirstOfMonthToToday() {
        when(repo.revenueByDay(any(), any())).thenReturn(List.of());
        RevenueReportResponse res = service.revenue(null, null, null);
        assertEquals(LocalDate.of(2026, 10, 1), res.from());
        assertEquals(LocalDate.of(2026, 10, 16), res.to());
        assertEquals(16, res.items().size());
    }

    @Test
    void queryRangeCoversWholeLastDay() {
        when(repo.revenueByDay(any(), any())).thenReturn(List.of());
        service.revenue("2026-10-01", "2026-10-03", null);
        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).revenueByDay(from.capture(), to.capture());
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), from.getValue());
        assertEquals(LocalDateTime.of(2026, 10, 4, 0, 0), to.getValue());
    }

    @Test
    void rejectsFromAfterTo() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.revenue("2026-10-10", "2026-10-01", null));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        verify(repo, never()).revenueByDay(any(), any());
    }

    @Test
    void rejectsRangeLongerThan366Days() {
        assertThrows(BusinessException.class, () -> service.revenue("2025-01-01", "2026-10-16", null));
        // đúng 366 ngày thì được
        when(repo.revenueByDay(any(), any())).thenReturn(List.of());
        service.revenue("2025-10-16", "2026-10-16", null);
    }

    @Test
    void rejectsBadDateFormat() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkinTraffic("16/10/2026", null));
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    // ---------- revenue ----------

    @Test
    void revenueByDay_splitsTypesAndZeroFills() {
        when(repo.revenueByDay(any(), any())).thenReturn(List.of(
                new RevenueRow(LocalDate.of(2026, 10, 2), "MEMBERSHIP", bd("500000.00")),
                new RevenueRow(LocalDate.of(2026, 10, 2), "POS", bd("45000.00")),
                new RevenueRow(LocalDate.of(2026, 10, 3), "POS", bd("30000.00"))));

        RevenueReportResponse res = service.revenue("2026-10-01", "2026-10-03", "day");

        assertEquals("DAY", res.groupBy());
        assertEquals(3, res.items().size());
        assertEquals(0, res.items().get(0).total().compareTo(BigDecimal.ZERO));        // 10-01 không có dữ liệu
        assertEquals(0, res.items().get(1).membership().compareTo(bd("500000")));
        assertEquals(0, res.items().get(1).pos().compareTo(bd("45000")));
        assertEquals(0, res.items().get(1).total().compareTo(bd("545000")));
        assertEquals(0, res.membershipTotal().compareTo(bd("500000")));
        assertEquals(0, res.posTotal().compareTo(bd("75000")));
        assertEquals(0, res.total().compareTo(bd("575000")));
    }

    @Test
    void revenueByMonth_aggregatesDaysIntoMonths() {
        when(repo.revenueByDay(any(), any())).thenReturn(List.of(
                new RevenueRow(LocalDate.of(2026, 9, 29), "MEMBERSHIP", bd("100")),
                new RevenueRow(LocalDate.of(2026, 9, 30), "MEMBERSHIP", bd("200")),
                new RevenueRow(LocalDate.of(2026, 10, 2), "POS", bd("50"))));

        RevenueReportResponse res = service.revenue("2026-09-01", "2026-10-16", "MONTH");

        assertEquals("MONTH", res.groupBy());
        assertEquals(2, res.items().size());
        assertEquals("2026-09", res.items().get(0).period());
        assertEquals(0, res.items().get(0).membership().compareTo(bd("300")));
        assertEquals("2026-10", res.items().get(1).period());
        assertEquals(0, res.items().get(1).pos().compareTo(bd("50")));
    }

    @Test
    void revenue_rejectsUnknownGroupBy() {
        assertThrows(BusinessException.class, () -> service.revenue(null, null, "WEEK"));
    }

    // ---------- renewal-rate ----------

    @Test
    void renewalRate_isNullWhenNothingExpired() {
        when(repo.renewalCounts(any(), any(), any(), anyInt())).thenReturn(new RenewalCounts(0, 0));
        RenewalRateResponse res = service.renewalRate(null, null, null);
        assertNull(res.rate());
        assertEquals(30, res.windowDays());
    }

    @Test
    void renewalRate_computesFourDecimalRatio() {
        when(repo.renewalCounts(any(), any(), any(), anyInt())).thenReturn(new RenewalCounts(4, 3));
        RenewalRateResponse res = service.renewalRate("2026-09-01", "2026-09-30", "14");
        assertEquals(0, res.rate().compareTo(bd("0.7500")));
        assertEquals(4, res.expiredCount());
        assertEquals(3, res.renewedCount());
        assertEquals(14, res.windowDays());
    }

    @Test
    void renewalRate_passesTodayInVietnamTimeAndRejectsBadWindow() {
        when(repo.renewalCounts(any(), any(), any(), anyInt())).thenReturn(new RenewalCounts(1, 1));
        service.renewalRate("2026-09-01", "2026-09-30", null);
        verify(repo).renewalCounts(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 10, 16), 30);

        assertThrows(BusinessException.class, () -> service.renewalRate(null, null, "0"));
        assertThrows(BusinessException.class, () -> service.renewalRate(null, null, "366"));
        assertThrows(BusinessException.class, () -> service.renewalRate(null, null, "abc"));
    }

    // ---------- checkin-traffic ----------

    @Test
    void checkinTraffic_fillsAll24HoursAndDays() {
        when(repo.checkinTraffic(any(), any())).thenReturn(List.of(
                new TrafficRow(LocalDate.of(2026, 10, 1), 7, 3),
                new TrafficRow(LocalDate.of(2026, 10, 1), 18, 5),
                new TrafficRow(LocalDate.of(2026, 10, 2), 18, 2)));

        CheckinTrafficResponse res = service.checkinTraffic("2026-10-01", "2026-10-03");

        assertEquals(10, res.total());
        assertEquals(3, res.byDay().size());
        assertEquals(8, res.byDay().get(0).count());
        assertEquals(2, res.byDay().get(1).count());
        assertEquals(0, res.byDay().get(2).count());
        assertEquals(24, res.byHour().size());
        assertEquals(3, res.byHour().get(7).count());
        assertEquals(7, res.byHour().get(18).count());
        assertEquals(0, res.byHour().get(0).count());
    }

    // ---------- top-packages ----------

    @Test
    void topPackages_defaultsLimitTo10AndMapsRows() {
        UUID id = UUID.randomUUID();
        when(repo.topPackages(any(), any(), anyInt())).thenReturn(List.of(
                new TopPackageRow(id, "Gói 3 tháng", 12, bd("18000000.00"))));

        TopPackagesResponse res = service.topPackages(null, null, null);

        verify(repo).topPackages(any(), any(), org.mockito.ArgumentMatchers.eq(10));
        assertEquals(1, res.items().size());
        assertEquals(id, res.items().get(0).packageId());
        assertEquals(12, res.items().get(0).registrations());
    }

    @Test
    void topPackages_rejectsLimitOutOfRange() {
        assertThrows(BusinessException.class, () -> service.topPackages(null, null, "0"));
        assertThrows(BusinessException.class, () -> service.topPackages(null, null, "51"));
        verify(repo, never()).topPackages(any(), any(), anyInt());
    }
}

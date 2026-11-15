package com.gym.management.service.impl;

import com.gym.management.dto.response.BookingResponse;
import com.gym.management.dto.response.CheckInHistoryItem;
import com.gym.management.dto.response.MemberStatsResponse;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.MemberPackage;
import com.gym.management.entity.Package;
import com.gym.management.service.BookingService;
import com.gym.management.service.CheckInHistoryService;
import com.gym.management.service.MembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberStatsServiceImplTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String MEMBER = "MEMBER";

    @Mock private MembershipService membershipService;
    @Mock private BookingService bookingService;
    @Mock private CheckInHistoryService checkInHistoryService;

    private MemberStatsServiceImpl service;
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // 2026-11-13 10:00 giờ VN
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T03:00:00Z"), VN);
        service = new MemberStatsServiceImpl(membershipService, bookingService, checkInHistoryService, clock);
    }

    private static BookingResponse booking(BookingStatus status) {
        return BookingResponse.builder().status(status).build();
    }

    private static MemberPackage pack(Integer remaining) {
        Package p = new Package();
        p.setName("Gói PT 12 buổi");
        MemberPackage mp = new MemberPackage();
        mp.setGymPackage(p);
        mp.setRemainingSessions(remaining);
        mp.setEndDate(LocalDate.of(2026, 12, 31));
        return mp;
    }

    private void stubBookings(List<BookingResponse> window, int completedTotal, int pending, int confirmed) {
        LocalDate to = LocalDate.of(2026, 11, 13);
        LocalDate from = to.minusDays(29);
        when(bookingService.findMyBookings(memberId, MEMBER, null, from, to)).thenReturn(window);
        when(bookingService.findMyBookings(eq(memberId), eq(MEMBER), eq(BookingStatus.COMPLETED), isNull(), isNull()))
                .thenReturn(java.util.Collections.nCopies(completedTotal, booking(BookingStatus.COMPLETED)));
        when(bookingService.findMyBookings(eq(memberId), eq(MEMBER), eq(BookingStatus.PENDING), isNull(), isNull()))
                .thenReturn(java.util.Collections.nCopies(pending, booking(BookingStatus.PENDING)));
        when(bookingService.findMyBookings(eq(memberId), eq(MEMBER), eq(BookingStatus.CONFIRMED), isNull(), isNull()))
                .thenReturn(java.util.Collections.nCopies(confirmed, booking(BookingStatus.CONFIRMED)));
    }

    private static CheckInHistoryItem visit(LocalDateTime t) {
        return CheckInHistoryItem.builder().type(CheckInHistoryItem.TYPE_GYM_VISIT).id(UUID.randomUUID()).checkInTime(t).build();
    }

    @Test
    void countsWindowStatusesAndAvailableSessions() {
        stubBookings(List.of(
                booking(BookingStatus.COMPLETED), booking(BookingStatus.COMPLETED),
                booking(BookingStatus.CONFIRMED), booking(BookingStatus.LATE_CANCELLED)), 9, 1, 2);
        when(checkInHistoryService.findMyHistory(memberId)).thenReturn(List.of(
                visit(LocalDateTime.of(2026, 11, 13, 6, 0)),   // hôm nay: tính
                visit(LocalDateTime.of(2026, 10, 15, 6, 0)),   // ngày đầu cửa sổ: tính
                visit(LocalDateTime.of(2026, 10, 14, 23, 59)))); // ngoài cửa sổ: bỏ
        when(membershipService.getCurrentMembership(memberId)).thenReturn(Optional.of(pack(5)));

        MemberStatsResponse r = service.getMyStats(memberId);

        assertEquals(LocalDate.of(2026, 10, 15), r.getFrom());
        assertEquals(LocalDate.of(2026, 11, 13), r.getTo());
        assertTrue(r.isHasActiveMembership());
        assertEquals("Gói PT 12 buổi", r.getPackageName());
        assertEquals(5, r.getRemainingSessions());
        assertEquals(3, r.getReservedSessions());
        assertEquals(2, r.getAvailableSessions());
        assertEquals(9, r.getCompletedSessionsTotal());
        assertEquals(2, r.getCompletedSessionsLast30Days());
        assertEquals(2, r.getGymVisitsLast30Days());
        assertEquals(7, r.getBookingsByStatusLast30Days().size());
        assertEquals(2, r.getBookingsByStatusLast30Days().get("COMPLETED"));
        assertEquals(1, r.getBookingsByStatusLast30Days().get("CONFIRMED"));
        assertEquals(1, r.getBookingsByStatusLast30Days().get("LATE_CANCELLED"));
        assertEquals(0, r.getBookingsByStatusLast30Days().get("PENDING"));
    }

    @Test
    void availableSessionsNeverNegative() {
        stubBookings(List.of(), 0, 2, 2);
        when(checkInHistoryService.findMyHistory(memberId)).thenReturn(List.of());
        when(membershipService.getCurrentMembership(memberId)).thenReturn(Optional.of(pack(1)));

        MemberStatsResponse r = service.getMyStats(memberId);

        assertEquals(4, r.getReservedSessions());
        assertEquals(0, r.getAvailableSessions());
    }

    @Test
    void unlimitedPackageHasNullRemainingAndAvailable() {
        stubBookings(List.of(), 0, 0, 1);
        when(checkInHistoryService.findMyHistory(memberId)).thenReturn(List.of());
        when(membershipService.getCurrentMembership(memberId)).thenReturn(Optional.of(pack(null)));

        MemberStatsResponse r = service.getMyStats(memberId);

        assertTrue(r.isHasActiveMembership());
        assertNull(r.getRemainingSessions());
        assertNull(r.getAvailableSessions());
        assertEquals(1, r.getReservedSessions());
    }

    @Test
    void noActiveMembershipStillReturnsActivityStats() {
        stubBookings(List.of(booking(BookingStatus.NO_SHOW)), 3, 0, 0);
        when(checkInHistoryService.findMyHistory(memberId)).thenReturn(List.of());
        when(membershipService.getCurrentMembership(memberId)).thenReturn(Optional.empty());

        MemberStatsResponse r = service.getMyStats(memberId);

        assertFalse(r.isHasActiveMembership());
        assertNull(r.getPackageName());
        assertNull(r.getRemainingSessions());
        assertNull(r.getAvailableSessions());
        assertEquals(3, r.getCompletedSessionsTotal());
        assertEquals(1, r.getBookingsByStatusLast30Days().get("NO_SHOW"));
    }
}

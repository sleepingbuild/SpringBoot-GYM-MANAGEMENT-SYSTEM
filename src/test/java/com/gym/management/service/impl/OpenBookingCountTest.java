package com.gym.management.service.impl;

import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.MembershipLookupPort;
import com.gym.management.service.TrainerScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Số buổi giữ chỗ cho Agent 2: đếm PENDING/CONFIRMED sau khi áp lazy-status, cùng cách đếm với luật đặt lịch. */
class OpenBookingCountTest {

    /** 10:00 ngày 10/11/2026 giờ Việt Nam. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 11);

    private final UUID memberId = UUID.randomUUID();

    private BookingRepository bookingRepository;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        service = new BookingServiceImpl(bookingRepository, mock(UserRepository.class),
                mock(TrainerScheduleService.class), mock(MembershipLookupPort.class),
                mock(ApplicationEventPublisher.class), CLOCK);
        ReflectionTestUtils.setField(service, "minLeadMinutes", 30);
        ReflectionTestUtils.setField(service, "pendingCancelThresholdMinutes", 60);
        ReflectionTestUtils.setField(service, "noShowGraceMinutes", 15);
        ReflectionTestUtils.setField(service, "lateCancelHours", 2);
    }

    private Booking booking(BookingStatus status, LocalDate date, LocalTime start) {
        return Booking.builder()
                .bookingDate(date)
                .startTime(start)
                .endTime(start.plusHours(1))
                .status(status)
                .build();
    }

    @Test
    void noOpenBookings_isZero() {
        when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of());

        assertEquals(0, service.countOpenBookings(memberId));
    }

    @Test
    void countsPendingAndConfirmed() {
        when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of(
                booking(BookingStatus.PENDING, TOMORROW, LocalTime.of(9, 0)),
                booking(BookingStatus.CONFIRMED, TOMORROW, LocalTime.of(14, 0))));

        assertEquals(2, service.countOpenBookings(memberId));
    }

    @Test
    void stalePendingBooking_isNotCounted() {
        // Bây giờ 10:00; PENDING lúc 10:30 còn dưới 60 phút nên lazy tự huỷ, không còn giữ chỗ.
        when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of(
                booking(BookingStatus.PENDING, TODAY, LocalTime.of(10, 30)),
                booking(BookingStatus.CONFIRMED, TOMORROW, LocalTime.of(14, 0))));

        assertEquals(1, service.countOpenBookings(memberId));
    }

    @Test
    void confirmedBookingThatBecamePtNoShow_isNotCounted() {
        // CONFIRMED lúc 09:00 hôm nay, quá giờ + ân hạn 15 phút, PT chưa quét vào: lật sang PT_NO_SHOW.
        when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of(
                booking(BookingStatus.CONFIRMED, TODAY, LocalTime.of(9, 0))));

        assertEquals(0, service.countOpenBookings(memberId));
    }
}

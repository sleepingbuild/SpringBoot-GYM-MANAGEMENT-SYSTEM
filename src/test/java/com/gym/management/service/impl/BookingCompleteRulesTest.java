package com.gym.management.service.impl;

import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.MembershipLookupPort;
import com.gym.management.service.TrainerScheduleService;
import com.gym.management.service.event.BookingSettledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PT hoàn thành buổi: không cần quét mặt vào/ra; thời lượng tính từ lúc PT quét vào (chưa quét thì từ giờ bắt
 * đầu theo lịch) đến lúc PT quét ra (chưa quét thì đến lúc bấm). Dưới 45 phút thì bắt buộc có lý do.
 * Luật như nhau cho buổi trong ca và buổi ngoài giờ.
 */
class BookingCompleteRulesTest {

    /** 10:00 ngày 10/11/2026 giờ Việt Nam. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private final UUID trainerId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    private BookingRepository bookingRepository;
    private ApplicationEventPublisher eventPublisher;
    private BookingServiceImpl service;
    private User member;
    private User trainer;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new BookingServiceImpl(bookingRepository, mock(UserRepository.class),
                mock(TrainerScheduleService.class), mock(MembershipLookupPort.class), eventPublisher, CLOCK);
        ReflectionTestUtils.setField(service, "minLeadMinutes", 30);
        ReflectionTestUtils.setField(service, "pendingCancelThresholdMinutes", 60);
        ReflectionTestUtils.setField(service, "noShowGraceMinutes", 15);
        ReflectionTestUtils.setField(service, "lateCancelHours", 2);
        ReflectionTestUtils.setField(service, "minCompleteMinutes", 45);

        member = mock(User.class);
        when(member.getId()).thenReturn(memberId);
        trainer = mock(User.class);
        when(trainer.getId()).thenReturn(trainerId);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Booking booking(LocalTime start, LocalTime end) {
        Booking b = Booking.builder()
                .member(member).trainer(trainer)
                .bookingDate(TODAY).startTime(start).endTime(end)
                .status(BookingStatus.CONFIRMED)
                .build();
        b.setId(UUID.randomUUID());
        when(bookingRepository.findByIdAndTrainerId(b.getId(), trainerId)).thenReturn(Optional.of(b));
        return b;
    }

    private BookingSettledEvent settledEvent() {
        ArgumentCaptor<BookingSettledEvent> captor = ArgumentCaptor.forClass(BookingSettledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private void assertValidationError(Runnable action) {
        BusinessException ex = assertThrows(BusinessException.class, action::run);
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void fullSession_withoutPtScanOut_completesAndRecordsManualCheckOut() {
        Booking b = booking(LocalTime.of(9, 0), LocalTime.of(10, 0));
        b.setCheckInTime(TODAY.atTime(9, 1));
        b.setPtCheckInTime(TODAY.atTime(9, 0));

        service.completeBooking(trainerId, b.getId());

        assertEquals(BookingStatus.COMPLETED, b.getStatus());
        assertEquals(TODAY.atTime(10, 0), b.getPtCheckOutTime());
        assertEquals("MANUAL", b.getPtCheckOutMethod());
        BookingSettledEvent event = settledEvent();
        assertEquals(BookingSettledEvent.Outcome.COMPLETED, event.outcome());
        assertTrue(event.fullDuration());
        assertFalse(event.earlyLeaveWithReason()); // dạy đủ giờ: không phải ca "ra sớm có lý do"
    }

    @Test
    void under45Minutes_withoutReason_isRejectedAndNothingChanges() {
        Booking b = booking(LocalTime.of(9, 30), LocalTime.of(10, 30));
        b.setCheckInTime(TODAY.atTime(9, 31));
        b.setPtCheckInTime(TODAY.atTime(9, 30)); // làm được 30 phút tới 10:00

        assertValidationError(() -> service.completeBooking(trainerId, b.getId()));

        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertNull(b.getPtCheckOutTime());
        verify(eventPublisher, never()).publishEvent(any(BookingSettledEvent.class));
    }

    @Test
    void under45Minutes_withReason_completesAndKeepsTrimmedReason() {
        Booking b = booking(LocalTime.of(9, 30), LocalTime.of(10, 30));
        b.setCheckInTime(TODAY.atTime(9, 31));
        b.setPtCheckInTime(TODAY.atTime(9, 30));

        service.completeBooking(trainerId, b.getId(), "  Hoc vien xin ve som  ");

        assertEquals(BookingStatus.COMPLETED, b.getStatus());
        assertEquals("Hoc vien xin ve som", b.getPtEarlyLeaveReason());
        BookingSettledEvent event = settledEvent();
        assertTrue(event.fullDuration());
        assertTrue(event.earlyLeaveWithReason()); // quét ra trước 10:30 và có lý do: Agent 5 ghi nhận cờ này
    }

    @Test
    void tooShortReason_isRejected_evenForFullSession() {
        Booking b = booking(LocalTime.of(9, 0), LocalTime.of(10, 0));
        b.setCheckInTime(TODAY.atTime(9, 1));
        b.setPtCheckInTime(TODAY.atTime(9, 0));

        assertValidationError(() -> service.completeBooking(trainerId, b.getId(), "ab"));

        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
    }

    @Test
    void over45ButBeforeScheduledEnd_withoutReason_completesButIsNotFullDuration() {
        Booking b = booking(LocalTime.of(9, 0), LocalTime.of(11, 0)); // lịch tới 11:00, bấm lúc 10:00 = 60 phút
        b.setCheckInTime(TODAY.atTime(9, 1));
        b.setPtCheckInTime(TODAY.atTime(9, 0));

        service.completeBooking(trainerId, b.getId());

        assertEquals(BookingStatus.COMPLETED, b.getStatus());
        BookingSettledEvent event = settledEvent();
        assertFalse(event.fullDuration()); // giữ luật cũ: chưa đủ giờ, không lý do thì không tính công
        assertFalse(event.earlyLeaveWithReason());
    }

    @Test
    void alreadyScannedOutUnder45_needsReason() {
        Booking b = booking(LocalTime.of(9, 0), LocalTime.of(10, 0));
        b.setCheckInTime(TODAY.atTime(9, 1));
        b.setPtCheckInTime(TODAY.atTime(9, 0));
        b.setPtCheckOutTime(TODAY.atTime(9, 40)); // 40 phút

        assertValidationError(() -> service.completeBooking(trainerId, b.getId()));
    }

    @Test
    void alreadyScannedOutUnder45_withReasonSavedEarlier_completes() {
        Booking b = booking(LocalTime.of(9, 0), LocalTime.of(10, 0));
        b.setCheckInTime(TODAY.atTime(9, 1));
        b.setPtCheckInTime(TODAY.atTime(9, 0));
        b.setPtCheckOutTime(TODAY.atTime(9, 40));
        b.setPtEarlyLeaveReason("Hoc vien xin ve som");

        service.completeBooking(trainerId, b.getId());

        assertEquals(BookingStatus.COMPLETED, b.getStatus());
        assertTrue(settledEvent().fullDuration());
    }

    @Test
    void withoutPtScanIn_usesScheduledStart_andNeedsReasonWhenShort() {
        // Buổi 09:50-10:50, bây giờ 10:00 còn trong ân hạn nên chưa bị lật PT_NO_SHOW. Tính từ 09:50 = 10 phút.
        Booking b = booking(LocalTime.of(9, 50), LocalTime.of(10, 50));
        b.setCheckInTime(TODAY.atTime(9, 51));

        assertValidationError(() -> service.completeBooking(trainerId, b.getId()));

        service.completeBooking(trainerId, b.getId(), "Hoc vien chi tap ngan");
        assertEquals(BookingStatus.COMPLETED, b.getStatus());
    }

    @Test
    void memberNotCheckedIn_isStillRejected() {
        // Bây giờ 10:00, buổi 09:50 mới quá giờ bắt đầu 10 phút nên còn trong thời gian chờ (chưa bị lật NO_SHOW).
        // Nếu đặt buổi 09:00 thì đã quá 15 phút chờ và hệ thống tự ghi NO_SHOW trước khi tới kiểm tra này.
        Booking b = booking(LocalTime.of(9, 50), LocalTime.of(10, 50));
        b.setPtCheckInTime(TODAY.atTime(9, 50));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.completeBooking(trainerId, b.getId()));

        assertEquals(ErrorCode.BOOKING_NOT_CHECKED_IN, ex.getErrorCode());
    }
}

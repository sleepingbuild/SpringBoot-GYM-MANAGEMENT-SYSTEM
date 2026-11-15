package com.gym.management.service.impl;

import com.gym.management.dto.request.BookingRequest;
import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.MembershipLookupPort;
import com.gym.management.service.TrainerScheduleService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Đặt lịch tự xác nhận: trong ca của PT thì CONFIRMED ngay, trong khung ngoài giờ thì PENDING
 * (chờ PT xác nhận), giờ khác thì từ chối.
 */
class BookingAutoConfirmTest {

    /** 10:00 ngày 10/11/2026 giờ Việt Nam. */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDate DATE = LocalDate.of(2026, 11, 11);

    private final UUID memberId = UUID.randomUUID();
    private final UUID trainerId = UUID.randomUUID();

    private BookingRepository bookingRepository;
    private TrainerScheduleService trainerScheduleService;
    private MembershipLookupPort membershipLookupPort;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        trainerScheduleService = mock(TrainerScheduleService.class);
        membershipLookupPort = mock(MembershipLookupPort.class);
        service = new BookingServiceImpl(bookingRepository, userRepository, trainerScheduleService,
                membershipLookupPort, mock(ApplicationEventPublisher.class), CLOCK);
        ReflectionTestUtils.setField(service, "minLeadMinutes", 30);
        ReflectionTestUtils.setField(service, "pendingCancelThresholdMinutes", 60);
        ReflectionTestUtils.setField(service, "noShowGraceMinutes", 15);
        ReflectionTestUtils.setField(service, "lateCancelHours", 2);

        User member = mock(User.class);
        when(member.getId()).thenReturn(memberId);
        User trainer = mock(User.class);
        when(trainer.getId()).thenReturn(trainerId);
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(membershipLookupPort.resolveActiveMembership(memberId))
                .thenReturn(new MembershipLookupPort.ActiveMembership(null, null, null));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private BookingRequest request(LocalTime start, LocalTime end) {
        return BookingRequest.builder()
                .trainerId(trainerId)
                .bookingDate(DATE)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private Booking savedBooking() {
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void inWorkingHours_isConfirmedImmediately() {
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);
        when(trainerScheduleService.isWithinWorkingHours(trainerId, DATE, start, end)).thenReturn(true);

        service.createBooking(memberId, "MEMBER", request(start, end));

        assertEquals(BookingStatus.CONFIRMED, savedBooking().getStatus());
        verify(trainerScheduleService, never()).isWithinOffHours(any(), any());
    }

    @Test
    void outsideShiftButInOffHours_isPendingForTrainer() {
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(23, 0);
        when(trainerScheduleService.isWithinWorkingHours(trainerId, DATE, start, end)).thenReturn(false);
        when(trainerScheduleService.isWithinOffHours(start, end)).thenReturn(true);

        service.createBooking(memberId, "MEMBER", request(start, end));

        assertEquals(BookingStatus.PENDING, savedBooking().getStatus());
    }

    @Test
    void outsideShiftAndOutsideOffHours_isRejected() {
        LocalTime start = LocalTime.of(3, 0);
        LocalTime end = LocalTime.of(4, 0);
        when(trainerScheduleService.isWithinWorkingHours(trainerId, DATE, start, end)).thenReturn(false);
        when(trainerScheduleService.isWithinOffHours(start, end)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createBooking(memberId, "MEMBER", request(start, end)));

        assertEquals(ErrorCode.BOOKING_OUT_OF_WORKING_HOURS, ex.getErrorCode());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void offHoursBooking_stillNeedsRemainingSessions() {
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(23, 0);
        when(trainerScheduleService.isWithinOffHours(start, end)).thenReturn(true);
        // Còn 1 buổi nhưng đã có 1 booking đang mở: không đặt thêm được.
        when(membershipLookupPort.resolveActiveMembership(memberId))
                .thenReturn(new MembershipLookupPort.ActiveMembership(null, 1, null));
        Booking open = Booking.builder()
                .bookingDate(DATE)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .status(BookingStatus.PENDING)
                .build();
        when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of(open));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createBooking(memberId, "MEMBER", request(start, end)));

        assertEquals(ErrorCode.MEMBERSHIP_NO_SESSION, ex.getErrorCode());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void inWorkingHoursBooking_stillNeedsRemainingSessions() {
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);
        when(trainerScheduleService.isWithinWorkingHours(trainerId, DATE, start, end)).thenReturn(true);
        when(membershipLookupPort.resolveActiveMembership(memberId))
                .thenReturn(new MembershipLookupPort.ActiveMembership(null, 1, null));
        Booking open = Booking.builder()
                .bookingDate(DATE)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of(open));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createBooking(memberId, "MEMBER", request(start, end)));

        assertEquals(ErrorCode.MEMBERSHIP_NO_SESSION, ex.getErrorCode());
    }
}

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ⚠️ Test này cần BaseEntity/User thật có sẵn setId()/getId()/getFullName() (Lombok @Setter)
 * để compile — khớp bảng giả định đã được Agent 1 xác nhận trong Agent3-Respone.md mục 1.
 * User dùng mock(User.class) để không phụ thuộc cách khởi tạo thật của Agent 1 (builder/constructor).
 *
 * Mốc thời gian "now" cố định qua Clock.fixed(...) — KHÔNG dùng Clock.system(...) trong test,
 * đúng yêu cầu ở Agent3-Respone.md mục 6 ("Clock injectable để giả lập thời gian").
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private TrainerScheduleService trainerScheduleService;
    @Mock private MembershipLookupPort membershipLookupPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    private UUID memberId;
    private UUID trainerId;
    private User member;
    private User trainer;

    /** "Bây giờ" cố định = 08:00 sáng Thứ 2, 2026-10-05 (Asia/Ho_Chi_Minh), dùng chung mọi test. */
    private final LocalDate today = LocalDate.of(2026, 10, 5);
    private final Clock fixedClock = Clock.fixed(
            today.atTime(8, 0).atZone(ZONE).toInstant(), ZONE);

    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        trainerId = UUID.randomUUID();
        member = mock(User.class);
        trainer = mock(User.class);
        lenient().when(member.getId()).thenReturn(memberId);
        lenient().when(member.getFullName()).thenReturn("Hội viên A");
        lenient().when(trainer.getId()).thenReturn(trainerId);
        lenient().when(trainer.getFullName()).thenReturn("PT B");

        lenient().when(userRepository.findById(memberId)).thenReturn(Optional.of(member));
        lenient().when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));

        // Mặc định: mọi khung giờ đều "trong ca làm việc" và không bị chiếm — từng test
        // Nested class dưới đây override lại khi cần kiểm tra đúng rule đó.
        lenient().when(trainerScheduleService.isWithinWorkingHours(any(), any(), any(), any())).thenReturn(true);
        lenient().when(bookingRepository.findActiveByTrainerSlot(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(bookingRepository.findActiveByMemberSlot(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(membershipLookupPort.resolveMaxSessionsPerWeek(any())).thenReturn(null); // unlimited mặc định
        lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new BookingServiceImpl(
                bookingRepository, userRepository, trainerScheduleService,
                membershipLookupPort, eventPublisher, fixedClock);
        ReflectionTestUtils.setField(service, "minLeadMinutes", 30);
        ReflectionTestUtils.setField(service, "pendingCancelThresholdMinutes", 60);
        ReflectionTestUtils.setField(service, "ptNoShowGraceMinutes", 30);
    }

    private BookingRequest requestAt(LocalTime start, LocalTime end) {
        return BookingRequest.builder()
                .trainerId(trainerId)
                .bookingDate(today)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private Booking bookingAt(BookingStatus status, LocalTime start, LocalTime end) {
        Booking b = Booking.builder()
                .member(member)
                .trainer(trainer)
                .bookingDate(today)
                .startTime(start)
                .endTime(end)
                .status(status)
                .build();
        b.setId(UUID.randomUUID());
        return b;
    }

    // ================================================================
    // Rule 1: BOOKING_TOO_SOON
    // ================================================================
    @Nested
    class TooSoonRule {

        @Test
        void rejects_whenStartIsInThePast() {
            BookingRequest req = requestAt(LocalTime.of(7, 0), LocalTime.of(8, 0)); // đã qua (now = 08:00)

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_TOO_SOON);
        }

        @Test
        void rejects_whenStartIsLessThan30MinutesAway() {
            BookingRequest req = requestAt(LocalTime.of(8, 15), LocalTime.of(9, 15)); // còn 15 phút

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_TOO_SOON);
        }

        @Test
        void accepts_whenStartIsExactly30MinutesAway() {
            BookingRequest req = requestAt(LocalTime.of(8, 30), LocalTime.of(9, 30));

            assertThat(service.createBooking(memberId, "MEMBER", req)).isNotNull();
        }
    }

    // ================================================================
    // Rule 2: BOOKING_OUT_OF_WORKING_HOURS
    // ================================================================
    @Test
    void rejects_whenOutsideTrainerWorkingHours() {
        when(trainerScheduleService.isWithinWorkingHours(any(), any(), any(), any())).thenReturn(false);
        BookingRequest req = requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_OUT_OF_WORKING_HOURS);
    }

    // ================================================================
    // Rule 3: BOOKING_SLOT_TAKEN
    // ================================================================
    @Test
    void rejects_whenSlotAlreadyTakenForSameTrainer() {
        when(bookingRepository.findActiveByTrainerSlot(eq(trainerId), eq(today), eq(LocalTime.of(9, 0)), any()))
                .thenReturn(List.of(bookingAt(BookingStatus.PENDING, LocalTime.of(9, 0), LocalTime.of(10, 0))));
        BookingRequest req = requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_SLOT_TAKEN);
    }

    // ================================================================
    // Rule 4: BOOKING_DOUBLE_BOOKED
    // ================================================================
    @Test
    void rejects_whenMemberAlreadyHasAnotherTrainerAtSameSlot() {
        when(bookingRepository.findActiveByMemberSlot(eq(memberId), eq(today), eq(LocalTime.of(9, 0)), any()))
                .thenReturn(List.of(bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 0), LocalTime.of(10, 0))));
        BookingRequest req = requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_DOUBLE_BOOKED);
    }

    // ================================================================
    // Rule 5: BOOKING_WEEKLY_LIMIT_EXCEEDED
    // ================================================================
    @Nested
    class WeeklyLimitRule {

        @Test
        void rejects_whenPackageForbidsPtBookingAtAll() {
            when(membershipLookupPort.resolveMaxSessionsPerWeek(memberId)).thenReturn(0);
            BookingRequest req = requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0));

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_WEEKLY_LIMIT_EXCEEDED);
        }

        @Test
        void rejects_whenAlreadyReachedWeeklyMax() {
            when(membershipLookupPort.resolveMaxSessionsPerWeek(memberId)).thenReturn(2);
            // today = Thứ 2 2026-10-05 -> tuần = 2026-10-05 .. 2026-10-11
            when(bookingRepository.countActiveInWeek(eq(memberId),
                    eq(LocalDate.of(2026, 10, 5)), eq(LocalDate.of(2026, 10, 11)), any()))
                    .thenReturn(2L);
            BookingRequest req = requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0));

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_WEEKLY_LIMIT_EXCEEDED);
        }

        @Test
        void accepts_whenUnderWeeklyMax() {
            when(membershipLookupPort.resolveMaxSessionsPerWeek(memberId)).thenReturn(3);
            when(bookingRepository.countActiveInWeek(eq(memberId), any(), any(), any())).thenReturn(1L);
            BookingRequest req = requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0));

            assertThat(service.createBooking(memberId, "MEMBER", req)).isNotNull();
        }

        @Test
        void accepts_whenPackageHasNoLimit() {
            when(membershipLookupPort.resolveMaxSessionsPerWeek(memberId)).thenReturn(null);
            BookingRequest req = requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0));

            assertThat(service.createBooking(memberId, "MEMBER", req)).isNotNull();
            verify(bookingRepository, never()).countActiveInWeek(any(), any(), any(), any());
        }
    }

    // ================================================================
    // Lazy auto-status (Issue 3.4) — dùng getById() để trigger applyLazyStatus()
    // ================================================================
    @Nested
    class LazyAutoStatus {

        @Test
        void pendingBecomesCancelled_whenWithin60MinutesOfStartAndNotConfirmed() {
            // now = 08:00, start = 08:45 -> còn 45 phút (<=60) -> CANCELLED
            Booking booking = bookingAt(BookingStatus.PENDING, LocalTime.of(8, 45), LocalTime.of(9, 45));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            var response = service.getById(booking.getId());

            assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(bookingRepository).save(booking);
        }

        @Test
        void pendingStaysPending_whenMoreThan60MinutesBeforeStart() {
            // now = 08:00, start = 10:00 -> còn 120 phút -> không đổi
            Booking booking = bookingAt(BookingStatus.PENDING, LocalTime.of(10, 0), LocalTime.of(11, 0));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            var response = service.getById(booking.getId());

            assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
            verify(bookingRepository, never()).save(any());
        }

        @Test
        void confirmedBecomesNoShow_whenPastEndTimeWithoutCheckIn() {
            // now = 08:00, end = 07:30 (đã qua), chưa check-in -> NO_SHOW
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 0), LocalTime.of(7, 30));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            var response = service.getById(booking.getId());

            assertThat(response.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        }

        @Test
        void confirmedBecomesPtNoShow_whenCheckedInButNotCompleted31MinutesAfterStart() {
            // now = 08:00, start = 07:20 (quá 30p = 07:50 đã qua), end = 09:00 (chưa qua),
            // ĐÃ check-in -> không phải NO_SHOW (member có mặt) -> PT_NO_SHOW (PT quên hoàn thành)
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 20), LocalTime.of(9, 0));
            booking.setCheckInTime(today.atTime(7, 22));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            var response = service.getById(booking.getId());

            assertThat(response.getStatus()).isEqualTo(BookingStatus.PT_NO_SHOW);
        }

        @Test
        void noShowTakesPriorityOverPtNoShow_whenBothConditionsTrueAndNeverCheckedIn() {
            // now = 08:00, start = 07:00 (>30p qua), end = 07:40 (đã qua), chưa check-in
            // -> cả 2 điều kiện đều đúng, nhưng NO_SHOW phải thắng (Agent3-Respone.md mục 3.2: duyệt)
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 0), LocalTime.of(7, 40));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            var response = service.getById(booking.getId());

            assertThat(response.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        }

        @Test
        void terminalStatusIsNeverReEvaluated() {
            Booking booking = bookingAt(BookingStatus.COMPLETED, LocalTime.of(0, 0), LocalTime.of(1, 0));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            service.getById(booking.getId());

            verify(bookingRepository, never()).save(any());
        }
    }

    // ================================================================
    // complete() — chặn server nếu chưa check-in (REQUIREMENTS.md mục 3)
    // ================================================================
    @Test
    void complete_throwsNotCheckedIn_whenCheckInTimeIsNull() {
        Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(bookingRepository.findByIdAndTrainerId(booking.getId(), trainerId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.completeBooking(trainerId, booking.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_NOT_CHECKED_IN);
    }

    @Test
    void complete_publishesBookingCompletedEvent_whenCheckedIn() {
        Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 0), LocalTime.of(10, 0));
        booking.setCheckInTime(today.atTime(9, 1));
        when(bookingRepository.findByIdAndTrainerId(booking.getId(), trainerId)).thenReturn(Optional.of(booking));

        service.completeBooking(trainerId, booking.getId());

        verify(eventPublisher).publishEvent(any(com.gym.management.service.event.BookingCompletedEvent.class));
    }

    // ================================================================
    // cancel() — không huỷ được buổi đã diễn ra
    // ================================================================
    @Test
    void cancel_throwsPastCannotCancel_whenStartTimeHasPassed() {
        Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 0), LocalTime.of(8, 30)); // đã qua (now=08:00)
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.cancelBooking(memberId, "MEMBER", booking.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_PAST_CANNOT_CANCEL);
    }

    @Test
    void cancel_succeeds_whenBeforeStartTime() {
        Booking booking = bookingAt(BookingStatus.PENDING, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        var response = service.cancelBooking(memberId, "MEMBER", booking.getId());

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}

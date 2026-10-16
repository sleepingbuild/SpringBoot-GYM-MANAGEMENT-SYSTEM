package com.gym.management.service.impl;

import com.gym.management.dto.request.BookingRequest;
import com.gym.management.dto.request.BookingUpdateRequest;
import com.gym.management.dto.response.BookingResponse;
import com.gym.management.dto.response.TrainerScanResult;
import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.MembershipLookupPort;
import com.gym.management.service.MembershipLookupPort.ActiveMembership;
import com.gym.management.service.TrainerScheduleService;
import com.gym.management.service.event.BookingSettledEvent;
import com.gym.management.service.event.BookingSettledEvent.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * "Bây giờ" cố định = 08:00 Thứ 2, 2026-10-05 (Asia/Ho_Chi_Minh) qua Clock.fixed(...) cho mọi test.
 * Ân hạn no-show = 15 phút, huỷ muộn = dưới 2 giờ (CHANGE_PT_ATTENDANCE_AND_PAY.md).
 *
 * Lưu ý: query JPQL (chồng khoảng giờ, loại status) KHÔNG test được bằng mock — phần đó cần
 * test tích hợp với DB thật; ở đây chỉ kiểm tra service truyền đúng tham số vào repository.
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

    private final LocalDate today = LocalDate.of(2026, 10, 5);
    private final Clock fixedClock = Clock.fixed(today.atTime(8, 0).atZone(ZONE).toInstant(), ZONE);

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

        lenient().when(trainerScheduleService.isWithinWorkingHours(any(), any(), any(), any())).thenReturn(true);
        lenient().when(bookingRepository.findActiveByTrainerSlot(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(bookingRepository.findActiveByMemberSlot(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(membershipLookupPort.resolveActiveMembership(any())).thenReturn(membership(null, null, null)); // không giới hạn gì
        lenient().when(bookingRepository.findOpenByMember(any(), any())).thenReturn(List.of());
        lenient().when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new BookingServiceImpl(
                bookingRepository, userRepository, trainerScheduleService,
                membershipLookupPort, eventPublisher, fixedClock);
        ReflectionTestUtils.setField(service, "minLeadMinutes", 30);
        ReflectionTestUtils.setField(service, "pendingCancelThresholdMinutes", 60);
        ReflectionTestUtils.setField(service, "noShowGraceMinutes", 15);
        ReflectionTestUtils.setField(service, "lateCancelHours", 2);
    }

    private ActiveMembership membership(Integer maxPerWeek, Integer remaining, LocalDate endDate) {
        return new ActiveMembership(maxPerWeek, remaining, endDate);
    }

    private BookingRequest requestAt(LocalTime start, LocalTime end) {
        return BookingRequest.builder().trainerId(trainerId).bookingDate(today).startTime(start).endTime(end).build();
    }

    private Booking bookingAt(BookingStatus status, LocalTime start, LocalTime end) {
        Booking b = Booking.builder()
                .member(member).trainer(trainer)
                .bookingDate(today).startTime(start).endTime(end)
                .status(status)
                .build();
        b.setId(UUID.randomUUID());
        return b;
    }

    private BookingSettledEvent captureSettledEvent() {
        ArgumentCaptor<BookingSettledEvent> captor = ArgumentCaptor.forClass(BookingSettledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private void verifyNoSettledEvent() {
        verify(eventPublisher, never()).publishEvent(any(BookingSettledEvent.class));
    }

    // ================================================================
    // Rule 1: BOOKING_TOO_SOON
    // ================================================================
    @Nested
    class TooSoonRule {

        @Test
        void rejects_whenStartIsInThePast() {
            BookingRequest req = requestAt(LocalTime.of(7, 0), LocalTime.of(8, 0));

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", req))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_TOO_SOON);
        }

        @Test
        void rejects_whenStartIsLessThan30MinutesAway() {
            BookingRequest req = requestAt(LocalTime.of(8, 15), LocalTime.of(9, 15));

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
    // Rule 2-4: ngoài ca, slot, trùng 2 PT
    // ================================================================
    @Test
    void rejects_whenOutsideTrainerWorkingHours() {
        when(trainerScheduleService.isWithinWorkingHours(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_OUT_OF_WORKING_HOURS);
    }

    @Test
    void rejects_whenSlotAlreadyTakenForSameTrainer_andPassesEndTimeForOverlapCheck() {
        // Service phải truyền CẢ endTime để query tìm được booking chồng khoảng giờ.
        when(bookingRepository.findActiveByTrainerSlot(
                eq(trainerId), eq(today), eq(LocalTime.of(9, 0)), eq(LocalTime.of(10, 0)), any()))
                .thenReturn(List.of(bookingAt(BookingStatus.PENDING, LocalTime.of(8, 30), LocalTime.of(9, 30))));

        assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_SLOT_TAKEN);
    }

    @Test
    void rejects_whenMemberAlreadyHasAnotherTrainerOverlappingThisTime() {
        when(bookingRepository.findActiveByMemberSlot(
                eq(memberId), eq(today), eq(LocalTime.of(9, 0)), eq(LocalTime.of(10, 0)), any()))
                .thenReturn(List.of(bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 30), LocalTime.of(10, 30))));

        assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_DOUBLE_BOOKED);
    }

    @Test
    void create_neverPassesNullExcludeId_toRepository() {
        service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0)));

        verify(bookingRepository).findActiveByTrainerSlot(any(), any(), any(), any(), eq(BookingRepository.NO_EXCLUDE));
        verify(bookingRepository).findActiveByMemberSlot(any(), any(), any(), any(), eq(BookingRepository.NO_EXCLUDE));
    }

    // ================================================================
    // Rule 5: giới hạn buổi/tuần
    // ================================================================
    @Nested
    class WeeklyLimitRule {

        @Test
        void rejects_whenPackageForbidsPtBookingAtAll() {
            when(membershipLookupPort.resolveActiveMembership(memberId)).thenReturn(membership(0, null, null));

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_WEEKLY_LIMIT_EXCEEDED);
        }

        @Test
        void rejects_whenAlreadyReachedWeeklyMax() {
            when(membershipLookupPort.resolveActiveMembership(memberId)).thenReturn(membership(2, null, null));
            when(bookingRepository.countActiveInWeek(eq(memberId),
                    eq(LocalDate.of(2026, 10, 5)), eq(LocalDate.of(2026, 10, 11)), any()))
                    .thenReturn(2L);

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_WEEKLY_LIMIT_EXCEEDED);
        }

        @Test
        void accepts_whenUnderWeeklyMax() {
            when(membershipLookupPort.resolveActiveMembership(memberId)).thenReturn(membership(3, null, null));
            when(bookingRepository.countActiveInWeek(eq(memberId), any(), any(), any())).thenReturn(1L);

            assertThat(service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0)))).isNotNull();
        }

        @Test
        void accepts_whenPackageHasNoLimit() {
            when(membershipLookupPort.resolveActiveMembership(memberId)).thenReturn(membership(null, null, null));

            assertThat(service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0)))).isNotNull();
            verify(bookingRepository, never()).countActiveInWeek(any(), any(), any(), any());
        }
    }

    // ================================================================
    // Rule 6: hạn gói (end_date là ngày cuối cùng còn hiệu lực, đã bao gồm) — MEMBERSHIP_EXPIRED
    // ================================================================
    @Nested
    class MembershipExpiryRule {

        @Test
        void rejects_whenBookingDateIsAfterPackageEndDate() {
            when(membershipLookupPort.resolveActiveMembership(memberId))
                    .thenReturn(membership(null, null, today.minusDays(1)));

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0))))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.MEMBERSHIP_EXPIRED);
        }

        @Test
        void accepts_whenBookingDateIsExactlyPackageEndDate() {
            when(membershipLookupPort.resolveActiveMembership(memberId))
                    .thenReturn(membership(null, null, today));

            assertThat(service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0)))).isNotNull();
        }

        @Test
        void accepts_whenPackageHasNoEndDate() {
            when(membershipLookupPort.resolveActiveMembership(memberId))
                    .thenReturn(membership(null, null, null));

            assertThat(service.createBooking(memberId, "MEMBER", requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0)))).isNotNull();
        }

        @Test
        void update_alsoRejects_whenNewDateIsAfterPackageEndDate() {
            when(membershipLookupPort.resolveActiveMembership(memberId))
                    .thenReturn(membership(null, null, today));
            Booking existing = bookingAt(BookingStatus.PENDING, LocalTime.of(10, 0), LocalTime.of(11, 0));
            when(bookingRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
            BookingUpdateRequest request = BookingUpdateRequest.builder()
                    .trainerId(trainerId).bookingDate(today.plusDays(1))
                    .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build();

            assertThatThrownBy(() -> service.updateBooking(memberId, "MEMBER", existing.getId(), request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.MEMBERSHIP_EXPIRED);
        }
    }

    // ================================================================
    // Rule 7 (G7): gói tính theo buổi — remaining - số booking đang mở phải còn > 0
    // ================================================================
    @Nested
    class RemainingSessionsRule {

        private void packageWithRemaining(Integer remaining) {
            when(membershipLookupPort.resolveActiveMembership(memberId))
                    .thenReturn(membership(null, remaining, null));
        }

        private BookingRequest request() {
            return requestAt(LocalTime.of(9, 0), LocalTime.of(10, 0));
        }

        @Test
        void accepts_andNeverQueriesOpenBookings_whenPackageIsNotSessionBased() {
            packageWithRemaining(null);

            assertThat(service.createBooking(memberId, "MEMBER", request())).isNotNull();
            verify(bookingRepository, never()).findOpenByMember(any(), any());
        }

        @Test
        void accepts_whenOneSessionLeftAndNoOpenBooking() {
            packageWithRemaining(1);

            assertThat(service.createBooking(memberId, "MEMBER", request())).isNotNull();
            verify(bookingRepository).findOpenByMember(eq(memberId), eq(BookingRepository.NO_EXCLUDE));
        }

        @Test
        void rejects_whenOneSessionLeftButAlreadyHasOneOpenBooking() {
            packageWithRemaining(1);
            when(bookingRepository.findOpenByMember(eq(memberId), any()))
                    .thenReturn(List.of(bookingAt(BookingStatus.CONFIRMED, LocalTime.of(12, 0), LocalTime.of(13, 0))));

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", request()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.MEMBERSHIP_NO_SESSION);
        }

        @Test
        void rejects_whenNoSessionLeftEvenWithoutOpenBooking() {
            packageWithRemaining(0);

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", request()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.MEMBERSHIP_NO_SESSION);
        }

        @Test
        void accepts_whenThreeSessionsLeftAndTwoOpen() {
            packageWithRemaining(3);
            when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of(
                    bookingAt(BookingStatus.PENDING, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                    bookingAt(BookingStatus.CONFIRMED, LocalTime.of(12, 0), LocalTime.of(13, 0))));

            assertThat(service.createBooking(memberId, "MEMBER", request())).isNotNull();
        }

        @Test
        void rejects_whenTwoSessionsLeftAndTwoOpen() {
            packageWithRemaining(2);
            when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of(
                    bookingAt(BookingStatus.PENDING, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                    bookingAt(BookingStatus.CONFIRMED, LocalTime.of(12, 0), LocalTime.of(13, 0))));

            assertThatThrownBy(() -> service.createBooking(memberId, "MEMBER", request()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.MEMBERSHIP_NO_SESSION);
        }

        @Test
        void doesNotCount_pendingBookingThatLazyStatusJustCancelled() {
            // PENDING 08:30 với now = 08:00: còn dưới 60 phút nên lazy tự huỷ, buổi được trả lại
            packageWithRemaining(1);
            Booking stale = bookingAt(BookingStatus.PENDING, LocalTime.of(8, 30), LocalTime.of(9, 30));
            when(bookingRepository.findOpenByMember(eq(memberId), any())).thenReturn(List.of(stale));

            assertThat(service.createBooking(memberId, "MEMBER", request())).isNotNull();
            assertThat(stale.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }

        @Test
        void update_excludesTheBookingBeingEdited() {
            packageWithRemaining(1);
            Booking existing = bookingAt(BookingStatus.PENDING, LocalTime.of(10, 0), LocalTime.of(11, 0));
            when(bookingRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
            // chính booking đang sửa là booking mở duy nhất; repository đã loại nó nên trả list rỗng
            when(bookingRepository.findOpenByMember(eq(memberId), eq(existing.getId()))).thenReturn(List.of());
            BookingUpdateRequest request = BookingUpdateRequest.builder()
                    .trainerId(trainerId).bookingDate(today)
                    .startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(12, 0)).build();

            assertThat(service.updateBooking(memberId, "MEMBER", existing.getId(), request)).isNotNull();
            verify(bookingRepository).findOpenByMember(eq(memberId), eq(existing.getId()));
        }
    }

    // ================================================================
    // Lazy auto-status (ân hạn 15 phút tính từ giờ BẮT ĐẦU) — trigger qua getById()
    // now = 08:00
    // ================================================================
    @Nested
    class LazyAutoStatus {

        private BookingResponse read(Booking booking) {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            return service.getById(booking.getId());
        }

        @Test
        void pendingBecomesCancelled_whenWithin60MinutesOfStart() {
            Booking booking = bookingAt(BookingStatus.PENDING, LocalTime.of(8, 45), LocalTime.of(9, 45));

            assertThat(read(booking).getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(bookingRepository).save(booking);
            verifyNoSettledEvent(); // huỷ tự động không phạt, không phát sự kiện
        }

        @Test
        void pendingStaysPending_whenMoreThan60MinutesBeforeStart() {
            Booking booking = bookingAt(BookingStatus.PENDING, LocalTime.of(10, 0), LocalTime.of(11, 0));

            assertThat(read(booking).getStatus()).isEqualTo(BookingStatus.PENDING);
            verify(bookingRepository, never()).save(any());
        }

        @Test
        void confirmedStaysConfirmed_withinGracePeriod() {
            // start 07:50 -> S+15 = 08:05, now 08:00 còn trong ân hạn
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 50), LocalTime.of(8, 50));

            assertThat(read(booking).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository, never()).save(any());
        }

        @Test
        void confirmedStaysConfirmed_exactlyAtGraceBoundary() {
            // start 07:45 -> S+15 = 08:00 = now: so sánh nghiêm ngặt nên CHƯA lật
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 45), LocalTime.of(8, 45));

            assertThat(read(booking).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository, never()).save(any());
        }

        @Test
        void becomesPtNoShow_whenPastGraceAndPtNeverCheckedIn_memberCheckedIn() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 40), LocalTime.of(8, 40));
            booking.setCheckInTime(today.atTime(7, 41));

            assertThat(read(booking).getStatus()).isEqualTo(BookingStatus.PT_NO_SHOW);
            verifyNoSettledEvent(); // lỗi PT: không trừ buổi, không trả công
        }

        @Test
        void becomesPtNoShow_whenBothAbsent_ptHasPriority() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 40), LocalTime.of(8, 40));

            assertThat(read(booking).getStatus()).isEqualTo(BookingStatus.PT_NO_SHOW);
            verifyNoSettledEvent();
        }

        @Test
        void becomesNoShow_whenPastGraceAndPtCheckedInButMemberNot_publishesMemberNoShow() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 40), LocalTime.of(8, 40));
            booking.setPtCheckInTime(today.atTime(7, 38));

            assertThat(read(booking).getStatus()).isEqualTo(BookingStatus.NO_SHOW);

            BookingSettledEvent event = captureSettledEvent();
            assertThat(event.outcome()).isEqualTo(Outcome.MEMBER_NO_SHOW);
            assertThat(event.fullDuration()).isTrue();
            assertThat(event.trainerId()).isEqualTo(trainerId);
            assertThat(event.memberId()).isEqualTo(memberId);
        }

        @Test
        void staysConfirmed_whenBothCheckedIn_afterGrace() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 40), LocalTime.of(8, 40));
            booking.setPtCheckInTime(today.atTime(7, 38));
            booking.setCheckInTime(today.atTime(7, 41));

            assertThat(read(booking).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository, never()).save(any());
        }

        @Test
        void flippingTwiceNeverPublishesTwice() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 40), LocalTime.of(8, 40));
            booking.setPtCheckInTime(today.atTime(7, 38));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            service.getById(booking.getId());
            service.getById(booking.getId()); // lần 2: đã NO_SHOW (trạng thái cuối)

            verify(eventPublisher, times(1)).publishEvent(any(BookingSettledEvent.class));
        }

        @Test
        void terminalStatusIsNeverReEvaluated() {
            for (BookingStatus terminal : List.of(BookingStatus.COMPLETED, BookingStatus.LATE_CANCELLED,
                    BookingStatus.PT_NO_SHOW, BookingStatus.NO_SHOW, BookingStatus.CANCELLED)) {
                Booking booking = bookingAt(terminal, LocalTime.of(0, 0), LocalTime.of(1, 0));

                assertThat(read(booking).getStatus()).isEqualTo(terminal);
            }
            verify(bookingRepository, never()).save(any());
            verifyNoSettledEvent();
        }
    }

    // ================================================================
    // Huỷ: LATE_CANCELLED (mốc 2 giờ), huỷ PENDING không phạt
    // now = 08:00 -> mốc muộn là start < 10:00 (đúng 10:00 thì chưa muộn)
    // ================================================================
    @Nested
    class CancelRules {

        private void willFind(Booking booking) {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        }

        @Test
        void memberCancelsConfirmed90MinutesBefore_becomesLateCancelled_andPublishes() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 30), LocalTime.of(10, 30));
            willFind(booking);

            BookingResponse response = service.cancelBooking(memberId, "MEMBER", booking.getId());

            assertThat(response.getStatus()).isEqualTo(BookingStatus.LATE_CANCELLED);
            BookingSettledEvent event = captureSettledEvent();
            assertThat(event.outcome()).isEqualTo(Outcome.LATE_CANCEL);
            assertThat(event.fullDuration()).isTrue();
        }

        @Test
        void memberCancelsConfirmedJustUnder2Hours_isLate() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 59), LocalTime.of(10, 59));
            willFind(booking);

            assertThat(service.cancelBooking(memberId, "MEMBER", booking.getId()).getStatus())
                    .isEqualTo(BookingStatus.LATE_CANCELLED);
        }

        @Test
        void memberCancelsConfirmedExactly2HoursBefore_isNotLate() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(10, 0), LocalTime.of(11, 0));
            willFind(booking);

            assertThat(service.cancelBooking(memberId, "MEMBER", booking.getId()).getStatus())
                    .isEqualTo(BookingStatus.CANCELLED);
            verifyNoSettledEvent();
        }

        @Test
        void memberCancelsPending_closeToStart_isFree() {
            // 90 phút trước giờ bắt đầu: lazy chưa tự huỷ (ngưỡng 60 phút) — PENDING luôn miễn phí (G8)
            Booking booking = bookingAt(BookingStatus.PENDING, LocalTime.of(9, 30), LocalTime.of(10, 30));
            willFind(booking);

            assertThat(service.cancelBooking(memberId, "MEMBER", booking.getId()).getStatus())
                    .isEqualTo(BookingStatus.CANCELLED);
            verifyNoSettledEvent();
        }

        @Test
        void trainerCancelsConfirmedCloseToStart_isPlainCancelled_noPenalty() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 30), LocalTime.of(10, 30));
            willFind(booking);

            assertThat(service.cancelBooking(trainerId, "TRAINER", booking.getId()).getStatus())
                    .isEqualTo(BookingStatus.CANCELLED);
            verifyNoSettledEvent();
        }

        @Test
        void receptionistCancelsForMember_followsSameLateRule() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 30), LocalTime.of(10, 30));
            willFind(booking);

            assertThat(service.cancelBooking(UUID.randomUUID(), "RECEPTIONIST", booking.getId()).getStatus())
                    .isEqualTo(BookingStatus.LATE_CANCELLED);
            assertThat(captureSettledEvent().outcome()).isEqualTo(Outcome.LATE_CANCEL);
        }

        @Test
        void cancellingAlreadyLateCancelled_isRejected_andNeverPublishesAgain() {
            Booking booking = bookingAt(BookingStatus.LATE_CANCELLED, LocalTime.of(9, 30), LocalTime.of(10, 30));
            willFind(booking);

            assertThatThrownBy(() -> service.cancelBooking(memberId, "MEMBER", booking.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
            verifyNoSettledEvent();
        }

        @Test
        void rejects_whenStartTimeHasPassed() {
            Booking booking = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 0), LocalTime.of(8, 30));
            willFind(booking);

            assertThatThrownBy(() -> service.cancelBooking(memberId, "MEMBER", booking.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_PAST_CANNOT_CANCEL);
        }

        @Test
        void rejects_strangerWithoutStaffRole() {
            Booking booking = bookingAt(BookingStatus.PENDING, LocalTime.of(10, 0), LocalTime.of(11, 0));
            willFind(booking);

            assertThatThrownBy(() -> service.cancelBooking(UUID.randomUUID(), "MEMBER", booking.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.AUTH_FORBIDDEN_ROLE);
        }
    }

    // ================================================================
    // fullDuration (field tính động trên BookingResponse) — dùng booking COMPLETED (trạng thái cuối)
    // buổi 06:00-07:00, now 08:00
    // ================================================================
    @Nested
    class FullDuration {

        private BookingResponse read(Booking booking) {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            return service.getById(booking.getId());
        }

        private Booking completedSession() {
            return bookingAt(BookingStatus.COMPLETED, LocalTime.of(6, 0), LocalTime.of(7, 0));
        }

        @Test
        void null_whenPtHasNotCheckedOut() {
            assertThat(read(completedSession()).getFullDuration()).isNull();
        }

        @Test
        void true_whenPtLeavesAtEndTime() {
            Booking b = completedSession();
            b.setPtCheckOutTime(today.atTime(7, 0));

            assertThat(read(b).getFullDuration()).isTrue();
        }

        @Test
        void true_whenPtLeavesAfterEndTime() {
            Booking b = completedSession();
            b.setPtCheckOutTime(today.atTime(7, 10));

            assertThat(read(b).getFullDuration()).isTrue();
        }

        @Test
        void false_whenPtLeavesEarlyWithoutReason() {
            Booking b = completedSession();
            b.setPtCheckOutTime(today.atTime(6, 40));

            assertThat(read(b).getFullDuration()).isFalse();
        }

        @Test
        void false_whenPtLeavesEarlyWithBlankReason() {
            Booking b = completedSession();
            b.setPtCheckOutTime(today.atTime(6, 40));
            b.setPtEarlyLeaveReason("   ");

            assertThat(read(b).getFullDuration()).isFalse();
        }

        @Test
        void true_whenPtLeavesEarlyWithReason() {
            Booking b = completedSession();
            b.setPtCheckOutTime(today.atTime(6, 40));
            b.setPtEarlyLeaveReason("Học viên xin về sớm vì chấn thương");

            assertThat(read(b).getFullDuration()).isTrue();
        }
    }

    // ================================================================
    // complete()
    // ================================================================
    @Nested
    class CompleteRules {

        private void willFind(Booking booking) {
            when(bookingRepository.findByIdAndTrainerId(booking.getId(), trainerId)).thenReturn(Optional.of(booking));
        }

        private Booking readyToComplete() {
            // buổi 07:00-08:00 đã tới giờ kết thúc, cả hai đã quét vào nên không bị lazy lật
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 0), LocalTime.of(8, 0));
            b.setPtCheckInTime(today.atTime(6, 58));
            b.setCheckInTime(today.atTime(7, 1));
            return b;
        }

        @Test
        void throwsNotCheckedIn_whenMemberHasNotCheckedIn() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 0), LocalTime.of(10, 0));
            willFind(b);

            assertThatThrownBy(() -> service.completeBooking(trainerId, b.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_NOT_CHECKED_IN);
        }

        @Test
        void rejects_whenStatusIsNotConfirmed_evenIfMemberCheckedIn() {
            Booking b = bookingAt(BookingStatus.CANCELLED, LocalTime.of(9, 0), LocalTime.of(10, 0));
            b.setCheckInTime(today.atTime(9, 1));
            willFind(b);

            assertThatThrownBy(() -> service.completeBooking(trainerId, b.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verifyNoSettledEvent();
        }

        @Test
        void throwsPtNotCheckedIn_whenPtNeverScanned() {
            // start 07:50 -> S+15 = 08:05 > now 08:00: còn trong ân hạn nên lazy chưa lật sang PT_NO_SHOW
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 50), LocalTime.of(8, 50));
            b.setCheckInTime(today.atTime(7, 51)); // học viên đã quét, PT chưa quét gì
            willFind(b);

            assertThatThrownBy(() -> service.completeBooking(trainerId, b.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_PT_NOT_CHECKED_IN);
            assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verifyNoSettledEvent();
        }

        @Test
        void throwsPtNotCheckedIn_whenPtCheckedInButNotOut() {
            Booking b = readyToComplete(); // PT đã quét vào, chưa quét ra
            willFind(b);

            assertThatThrownBy(() -> service.completeBooking(trainerId, b.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_PT_NOT_CHECKED_IN);
            verifyNoSettledEvent();
        }

        @Test
        void memberNotCheckedInIsReportedBeforePtNotCheckedIn() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(9, 0), LocalTime.of(10, 0)); // cả hai chưa quét
            willFind(b);

            assertThatThrownBy(() -> service.completeBooking(trainerId, b.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_NOT_CHECKED_IN);
        }

        @Test
        void publishesCompleted_fullDurationTrue_whenPtLeftAtEnd() {
            Booking b = readyToComplete();
            b.setPtCheckOutTime(today.atTime(8, 0));
            willFind(b);

            assertThat(service.completeBooking(trainerId, b.getId()).getStatus()).isEqualTo(BookingStatus.COMPLETED);

            BookingSettledEvent event = captureSettledEvent();
            assertThat(event.outcome()).isEqualTo(Outcome.COMPLETED);
            assertThat(event.fullDuration()).isTrue();
        }

        @Test
        void publishesCompleted_fullDurationFalse_whenPtLeftEarlyWithoutReason() {
            Booking b = readyToComplete();
            b.setPtCheckOutTime(today.atTime(7, 30));
            willFind(b);

            service.completeBooking(trainerId, b.getId());

            assertThat(captureSettledEvent().fullDuration()).isFalse();
        }

        @Test
        void publishesCompleted_fullDurationTrue_whenPtLeftEarlyWithReason() {
            Booking b = readyToComplete();
            b.setPtCheckOutTime(today.atTime(7, 30));
            b.setPtEarlyLeaveReason("Học viên xin về sớm");
            willFind(b);

            service.completeBooking(trainerId, b.getId());

            assertThat(captureSettledEvent().fullDuration()).isTrue();
        }
    }

    // ================================================================
    // PT nộp lý do rời buổi sớm — now = 08:00
    // ================================================================
    @Nested
    class EarlyLeaveReason {

        private void willFind(Booking booking) {
            when(bookingRepository.findByIdAndTrainerId(booking.getId(), trainerId)).thenReturn(Optional.of(booking));
        }

        /** Buổi 07:30-08:30, cả hai đã quét vào nên không bị lazy lật; PT quét ra lúc 08:00 = sớm 30 phút. */
        private Booking leftEarly() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 30), LocalTime.of(8, 30));
            b.setPtCheckInTime(today.atTime(7, 28));
            b.setCheckInTime(today.atTime(7, 31));
            b.setPtCheckOutTime(today.atTime(8, 0));
            return b;
        }

        @Test
        void savesTrimmedReason_andMakesSessionFullDuration() {
            Booking b = leftEarly();
            willFind(b);

            BookingResponse response = service.submitEarlyLeaveReason(trainerId, b.getId(), "  Học viên xin về sớm  ");

            assertThat(b.getPtEarlyLeaveReason()).isEqualTo("Học viên xin về sớm");
            assertThat(response.getPtEarlyLeaveReason()).isEqualTo("Học viên xin về sớm");
            assertThat(response.getFullDuration()).isTrue();
            verify(bookingRepository).save(b);
        }

        @Test
        void overwritesPreviousReason() {
            Booking b = leftEarly();
            b.setPtEarlyLeaveReason("Lý do cũ nhập sai");
            willFind(b);

            service.submitEarlyLeaveReason(trainerId, b.getId(), "Lý do mới đúng hơn");

            assertThat(b.getPtEarlyLeaveReason()).isEqualTo("Lý do mới đúng hơn");
        }

        @Test
        void rejects_whenPtHasNotCheckedOut() {
            Booking b = leftEarly();
            b.setPtCheckOutTime(null);
            willFind(b);

            assertThatThrownBy(() -> service.submitEarlyLeaveReason(trainerId, b.getId(), "Có việc đột xuất"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_EARLY_LEAVE_REASON_NOT_ALLOWED);
            assertThat(b.getPtEarlyLeaveReason()).isNull();
        }

        @Test
        void rejects_whenPtLeftExactlyAtEndTime() {
            Booking b = leftEarly();
            b.setPtCheckOutTime(today.atTime(8, 30));
            willFind(b);

            assertThatThrownBy(() -> service.submitEarlyLeaveReason(trainerId, b.getId(), "Có việc đột xuất"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_EARLY_LEAVE_REASON_NOT_ALLOWED);
        }

        @Test
        void rejects_whenBookingAlreadyCompleted() {
            Booking b = leftEarly();
            b.setStatus(BookingStatus.COMPLETED);
            willFind(b);

            assertThatThrownBy(() -> service.submitEarlyLeaveReason(trainerId, b.getId(), "Có việc đột xuất"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.BOOKING_EARLY_LEAVE_REASON_NOT_ALLOWED);
        }

        @Test
        void rejects_whenBookingBelongsToAnotherTrainer() {
            UUID bookingId = UUID.randomUUID();
            when(bookingRepository.findByIdAndTrainerId(bookingId, trainerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitEarlyLeaveReason(trainerId, bookingId, "Có việc đột xuất"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        void rejects_reasonThatIsTooShortAfterTrim() {
            Booking b = leftEarly();
            willFind(b);

            assertThatThrownBy(() -> service.submitEarlyLeaveReason(trainerId, b.getId(), "  ab   "))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(b.getPtEarlyLeaveReason()).isNull();
        }

        @Test
        void rejects_nullReason() {
            Booking b = leftEarly();
            willFind(b);

            assertThatThrownBy(() -> service.submitEarlyLeaveReason(trainerId, b.getId(), null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }

    // ================================================================
    // Học viên quét mặt
    // ================================================================
    @Nested
    class MemberFaceScan {

        @Test
        void checksIn_intoConfirmedBooking() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(8, 0), LocalTime.of(9, 0));
            b.setPtCheckInTime(today.atTime(7, 58));
            when(bookingRepository.findTodayPendingCheckIn(memberId, today)).thenReturn(List.of(b));
            LocalDateTime scan = today.atTime(8, 1);

            Optional<BookingResponse> result = service.recordFaceScan(memberId, scan);

            assertThat(result).isPresent();
            assertThat(b.getCheckInTime()).isEqualTo(scan);
            assertThat(b.getCheckInMethod()).isEqualTo("FACE");
        }

        @Test
        void skipsBookingThatLazyFlippedToPtNoShow() {
            // start 07:40, PT chưa quét vào, now 08:00 > S+15 -> lật PT_NO_SHOW, không check-in được
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 40), LocalTime.of(8, 40));
            when(bookingRepository.findTodayPendingCheckIn(memberId, today)).thenReturn(List.of(b));
            when(bookingRepository.findTodayPendingCheckOut(memberId, today)).thenReturn(List.of());

            Optional<BookingResponse> result = service.recordFaceScan(memberId, today.atTime(8, 0));

            assertThat(result).isEmpty();
            assertThat(b.getStatus()).isEqualTo(BookingStatus.PT_NO_SHOW);
            assertThat(b.getCheckInTime()).isNull();
        }

        @Test
        void checksOut_whenAlreadyCheckedIn() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 30), LocalTime.of(8, 30));
            b.setPtCheckInTime(today.atTime(7, 28));
            b.setCheckInTime(today.atTime(7, 31));
            when(bookingRepository.findTodayPendingCheckIn(memberId, today)).thenReturn(List.of());
            when(bookingRepository.findTodayPendingCheckOut(memberId, today)).thenReturn(List.of(b));
            LocalDateTime scan = today.atTime(8, 0);

            assertThat(service.recordFaceScan(memberId, scan)).isPresent();
            assertThat(b.getCheckOutTime()).isEqualTo(scan);
        }

        @Test
        void returnsEmpty_whenNoBookingToday() {
            when(bookingRepository.findTodayPendingCheckIn(memberId, today)).thenReturn(List.of());
            when(bookingRepository.findTodayPendingCheckOut(memberId, today)).thenReturn(List.of());

            assertThat(service.recordFaceScan(memberId, today.atTime(8, 0))).isEmpty();
        }
    }

    // ================================================================
    // PT quét mặt (recordTrainerFaceScan) — now = 08:00
    // ================================================================
    @Nested
    class TrainerFaceScan {

        private void todaysBookings(Booking... bookings) {
            when(bookingRepository.findTodayConfirmedByTrainer(trainerId, today)).thenReturn(List.of(bookings));
        }

        @Test
        void checksIn_whenScanIs20MinutesBeforeStart() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(8, 20), LocalTime.of(9, 20));
            todaysBookings(b);
            LocalDateTime scan = today.atTime(8, 0);

            Optional<TrainerScanResult> result = service.recordTrainerFaceScan(trainerId, scan);

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo(TrainerScanResult.CHECK_IN);
            assertThat(result.get().earlyLeave()).isFalse();
            assertThat(b.getPtCheckInTime()).isEqualTo(scan);
            assertThat(b.getPtCheckInMethod()).isEqualTo("FACE");
        }

        @Test
        void checksIn_atExactly30MinutesBeforeStart() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(8, 30), LocalTime.of(9, 30));
            todaysBookings(b);

            assertThat(service.recordTrainerFaceScan(trainerId, today.atTime(8, 0))).isPresent();
        }

        @Test
        void returnsEmpty_whenScanIsMoreThan30MinutesBeforeStart() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(8, 31), LocalTime.of(9, 31));
            todaysBookings(b);

            assertThat(service.recordTrainerFaceScan(trainerId, today.atTime(8, 0))).isEmpty();
            assertThat(b.getPtCheckInTime()).isNull();
        }

        @Test
        void checksIn_10MinutesAfterStart_stillWithinGrace() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 50), LocalTime.of(8, 50));
            todaysBookings(b);

            Optional<TrainerScanResult> result = service.recordTrainerFaceScan(trainerId, today.atTime(8, 0));

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo(TrainerScanResult.CHECK_IN);
        }

        @Test
        void returnsEmpty_andBookingBecomesPtNoShow_whenScanIsPastGrace() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 40), LocalTime.of(8, 40));
            todaysBookings(b);

            assertThat(service.recordTrainerFaceScan(trainerId, today.atTime(8, 0))).isEmpty();
            assertThat(b.getStatus()).isEqualTo(BookingStatus.PT_NO_SHOW);
            assertThat(b.getPtCheckInTime()).isNull();
        }

        @Test
        void checksOut_flagsEarlyLeave_whenBeforeEndTime() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 30), LocalTime.of(8, 30));
            b.setPtCheckInTime(today.atTime(7, 28));
            b.setCheckInTime(today.atTime(7, 31));
            todaysBookings(b);
            LocalDateTime scan = today.atTime(8, 0);

            Optional<TrainerScanResult> result = service.recordTrainerFaceScan(trainerId, scan);

            assertThat(result).isPresent();
            assertThat(result.get().type()).isEqualTo(TrainerScanResult.CHECK_OUT);
            assertThat(result.get().earlyLeave()).isTrue();
            assertThat(b.getPtCheckOutTime()).isEqualTo(scan);
            assertThat(b.getPtCheckOutMethod()).isEqualTo("FACE");
        }

        @Test
        void checksOut_noEarlyLeave_whenAtEndTime() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 0), LocalTime.of(8, 0));
            b.setPtCheckInTime(today.atTime(6, 58));
            b.setCheckInTime(today.atTime(7, 1));
            todaysBookings(b);

            Optional<TrainerScanResult> result = service.recordTrainerFaceScan(trainerId, today.atTime(8, 0));

            assertThat(result.get().type()).isEqualTo(TrainerScanResult.CHECK_OUT);
            assertThat(result.get().earlyLeave()).isFalse();
        }

        @Test
        void backToBack_firstScanChecksOutPrevious_secondScanChecksInNext() {
            Booking a = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 0), LocalTime.of(8, 0));
            a.setPtCheckInTime(today.atTime(6, 58));
            a.setCheckInTime(today.atTime(7, 1));
            Booking next = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(8, 0), LocalTime.of(9, 0));
            todaysBookings(a, next);

            Optional<TrainerScanResult> first = service.recordTrainerFaceScan(trainerId, today.atTime(8, 0));
            assertThat(first.get().type()).isEqualTo(TrainerScanResult.CHECK_OUT);
            assertThat(first.get().booking().getId()).isEqualTo(a.getId());

            Optional<TrainerScanResult> second = service.recordTrainerFaceScan(trainerId, today.atTime(8, 0));
            assertThat(second.get().type()).isEqualTo(TrainerScanResult.CHECK_IN);
            assertThat(second.get().booking().getId()).isEqualTo(next.getId());
        }

        @Test
        void returnsEmpty_andPublishesMemberNoShow_whenMemberNeverCameAfterGrace() {
            Booking b = bookingAt(BookingStatus.CONFIRMED, LocalTime.of(7, 30), LocalTime.of(8, 30));
            b.setPtCheckInTime(today.atTime(7, 28)); // PT đã vào, học viên vắng, quá S+15 = 07:45
            todaysBookings(b);

            assertThat(service.recordTrainerFaceScan(trainerId, today.atTime(8, 0))).isEmpty();
            assertThat(b.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
            assertThat(captureSettledEvent().outcome()).isEqualTo(Outcome.MEMBER_NO_SHOW);
        }

        @Test
        void returnsEmpty_whenTrainerHasNoBookingToday() {
            todaysBookings();

            assertThat(service.recordTrainerFaceScan(trainerId, today.atTime(8, 0))).isEmpty();
        }
    }
}

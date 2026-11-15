package com.gym.management.service.impl;

import com.gym.management.dto.response.AvailableSlotResponse;
import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.TrainerSchedule;
import com.gym.management.entity.User;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.TrainerScheduleRepository;
import com.gym.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Khung ngoài giờ mặc định 21:00 đến 23:59 (hiển thị 24:00), slot 60 phút. */
class TrainerScheduleOffHoursTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 10:00 ngày 10/11/2026 giờ Việt Nam. */
    private static final Clock MORNING = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZONE);
    /** 21:10 ngày 10/11/2026 giờ Việt Nam. */
    private static final Clock NIGHT = Clock.fixed(Instant.parse("2026-11-10T14:10:00Z"), ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 11);

    private final UUID trainerId = UUID.randomUUID();

    private TrainerScheduleRepository scheduleRepository;
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(TrainerScheduleRepository.class);
        bookingRepository = mock(BookingRepository.class);
    }

    private TrainerScheduleServiceImpl serviceAt(Clock clock) {
        TrainerScheduleServiceImpl service = new TrainerScheduleServiceImpl(
                scheduleRepository, bookingRepository, mock(UserRepository.class), clock);
        ReflectionTestUtils.setField(service, "slotDurationMinutes", 60);
        return service;
    }

    private static List<String> ranges(List<AvailableSlotResponse> slots) {
        return slots.stream().map(s -> s.getStartTime() + "-" + s.getEndTime()).toList();
    }

    // ---- isWithinOffHours ----

    @Test
    void offHours_insideWindow_isTrue() {
        TrainerScheduleServiceImpl service = serviceAt(MORNING);
        assertTrue(service.isWithinOffHours(LocalTime.of(21, 0), LocalTime.of(22, 0)));
        assertTrue(service.isWithinOffHours(LocalTime.of(23, 0), LocalTime.of(23, 59)));
    }

    @Test
    void offHours_outsideWindow_isFalse() {
        TrainerScheduleServiceImpl service = serviceAt(MORNING);
        assertFalse(service.isWithinOffHours(LocalTime.of(20, 0), LocalTime.of(21, 0)), "ngay truoc khung");
        assertFalse(service.isWithinOffHours(LocalTime.of(20, 30), LocalTime.of(21, 30)), "gac qua dau khung");
        assertFalse(service.isWithinOffHours(LocalTime.of(1, 0), LocalTime.of(2, 0)), "giua dem");
        assertFalse(service.isWithinOffHours(LocalTime.of(7, 0), LocalTime.of(8, 0)), "sang som");
    }

    // ---- findOffHoursSlots ----

    @Test
    void offHoursSlots_emptyDay_listsThreeSlotsEndingAtMidnight() {
        List<AvailableSlotResponse> slots = serviceAt(MORNING).findOffHoursSlots(trainerId, TOMORROW);

        assertEquals(List.of("21:00-22:00", "22:00-23:00", "23:00-23:59"), ranges(slots));
    }

    @Test
    void offHoursSlots_skipsSlotAlreadyBooked() {
        Booking taken = Booking.builder()
                .bookingDate(TOMORROW)
                .startTime(LocalTime.of(21, 0))
                .endTime(LocalTime.of(22, 0))
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookingRepository.findByTrainerIdOrderByBookingDateDescStartTimeDesc(trainerId))
                .thenReturn(List.of(taken));

        List<AvailableSlotResponse> slots = serviceAt(MORNING).findOffHoursSlots(trainerId, TOMORROW);

        assertEquals(List.of("22:00-23:00", "23:00-23:59"), ranges(slots));
    }

    @Test
    void offHoursSlots_skipsSlotsInsideTrainersShift() {
        TrainerSchedule shift = TrainerSchedule.builder()
                .trainer(mock(User.class))
                .workDate(TOMORROW)
                .startTime(LocalTime.of(21, 0))
                .endTime(LocalTime.of(23, 0))
                .active(true)
                .build();
        when(scheduleRepository.findByTrainerIdAndWorkDateAndActiveTrue(trainerId, TOMORROW))
                .thenReturn(List.of(shift));

        List<AvailableSlotResponse> slots = serviceAt(MORNING).findOffHoursSlots(trainerId, TOMORROW);

        // 21-22 và 22-23 nằm trong ca nên đã có ở danh sách slot trong ca (tự xác nhận).
        assertEquals(List.of("23:00-23:59"), ranges(slots));
    }

    @Test
    void offHoursSlots_today_skipsSlotsTooCloseToNow() {
        // Bây giờ 21:10: slot 21:00 đã qua, 22:00 còn hơn 30 phút nên còn.
        List<AvailableSlotResponse> slots = serviceAt(NIGHT).findOffHoursSlots(trainerId, TODAY);

        assertEquals(List.of("22:00-23:00", "23:00-23:59"), ranges(slots));
    }

    @Test
    void offHoursSlots_releasedBookingDoesNotBlockSlot() {
        Booking cancelled = Booking.builder()
                .bookingDate(TOMORROW)
                .startTime(LocalTime.of(21, 0))
                .endTime(LocalTime.of(22, 0))
                .status(BookingStatus.CANCELLED)
                .build();
        when(bookingRepository.findByTrainerIdOrderByBookingDateDescStartTimeDesc(trainerId))
                .thenReturn(List.of(cancelled));

        List<AvailableSlotResponse> slots = serviceAt(MORNING).findOffHoursSlots(trainerId, TOMORROW);

        assertEquals(3, slots.size());
    }
}

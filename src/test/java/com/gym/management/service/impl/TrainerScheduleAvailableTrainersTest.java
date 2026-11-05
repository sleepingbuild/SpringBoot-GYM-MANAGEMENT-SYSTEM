package com.gym.management.service.impl;

import com.gym.management.dto.response.AvailableTrainerResponse;
import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.TrainerSchedule;
import com.gym.management.entity.User;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.TrainerScheduleRepository;
import com.gym.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainerScheduleAvailableTrainersTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock private TrainerScheduleRepository scheduleRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;

    private final LocalDate today = LocalDate.of(2026, 10, 5);
    private final LocalDate day = today.plusDays(1);
    private final Clock clock = Clock.fixed(today.atTime(8, 0).atZone(ZONE).toInstant(), ZONE);

    private TrainerScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainerScheduleServiceImpl(scheduleRepository, bookingRepository, userRepository, clock);
        ReflectionTestUtils.setField(service, "slotDurationMinutes", 60);
        when(bookingRepository.findByTrainerIdOrderByBookingDateDescStartTimeDesc(any())).thenReturn(List.of());
    }

    private User trainer(String name, String status) {
        User u = mock(User.class);
        UUID id = UUID.randomUUID();
        when(u.getId()).thenReturn(id);
        when(u.getFullName()).thenReturn(name);
        when(u.getStatus()).thenReturn(status);
        return u;
    }

    private TrainerSchedule shift(User trainer, int fromHour, int toHour) {
        return TrainerSchedule.builder()
                .trainer(trainer).workDate(day)
                .startTime(LocalTime.of(fromHour, 0)).endTime(LocalTime.of(toHour, 0))
                .build();
    }

    /** Gia lap ca lam viec: truy van theo ngay (danh sach PT) va truy van theo tung PT (tinh slot trong). */
    private void givenShifts(TrainerSchedule... shifts) {
        when(scheduleRepository.findByWorkDateAndActiveTrue(day)).thenReturn(List.of(shifts));
        for (TrainerSchedule s : shifts) {
            UUID id = s.getTrainer().getId();
            List<TrainerSchedule> own = Arrays.stream(shifts)
                    .filter(x -> x.getTrainer().getId().equals(id)).toList();
            when(scheduleRepository.findByTrainerIdAndWorkDateAndActiveTrue(id, day)).thenReturn(own);
        }
    }

    @Test
    void listsTrainersWithFreeSlotsSortedByNameWithSlotCount() {
        User b = trainer("Binh", "ACTIVE");
        User a = trainer("An", "ACTIVE");
        givenShifts(shift(b, 8, 10), shift(a, 8, 9));

        List<AvailableTrainerResponse> result = service.listAvailableTrainers(day);

        assertThat(result).extracting(AvailableTrainerResponse::getTrainerName).containsExactly("An", "Binh");
        assertThat(result).extracting(AvailableTrainerResponse::getAvailableSlotCount).containsExactly(1, 2);
    }

    @Test
    void excludesTrainerWhoseSlotsAreAllTaken() {
        User full = trainer("Full", "ACTIVE");
        givenShifts(shift(full, 8, 9));
        Booking taken = Booking.builder()
                .trainer(full).bookingDate(day)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .status(BookingStatus.CONFIRMED).build();
        when(bookingRepository.findByTrainerIdOrderByBookingDateDescStartTimeDesc(full.getId()))
                .thenReturn(List.of(taken));

        assertThat(service.listAvailableTrainers(day)).isEmpty();
    }

    @Test
    void excludesInactiveTrainerAndCountsMergedShiftsOnce() {
        User inactive = trainer("Nghi", "INACTIVE");
        User twoShifts = trainer("Hai Ca", "ACTIVE");
        givenShifts(shift(inactive, 8, 10), shift(twoShifts, 8, 9), shift(twoShifts, 14, 15));

        List<AvailableTrainerResponse> result = service.listAvailableTrainers(day);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTrainerName()).isEqualTo("Hai Ca");
        assertThat(result.get(0).getAvailableSlotCount()).isEqualTo(2);
    }
}
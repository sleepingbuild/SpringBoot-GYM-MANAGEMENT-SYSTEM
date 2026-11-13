package com.gym.management.service.impl;

import com.gym.management.dto.request.TrainerScheduleRequest;
import com.gym.management.entity.TrainerSchedule;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.TrainerScheduleRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.security.CurrentUserId;
import com.gym.management.security.CurrentUserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Quy tắc quyền trên ca làm việc: PT chỉ thao tác ca của mình, SUPER_ADMIN thao tác hộ được mọi PT. */
class TrainerScheduleServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 10:00 ngày 10/11/2026 giờ Việt Nam. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZONE);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 11);

    private final UUID me = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    private TrainerScheduleRepository scheduleRepository;
    private UserRepository userRepository;
    private TrainerScheduleServiceImpl service;
    private MockedStatic<CurrentUserRole> roleMock;
    private MockedStatic<CurrentUserId> idMock;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(TrainerScheduleRepository.class);
        userRepository = mock(UserRepository.class);
        service = new TrainerScheduleServiceImpl(scheduleRepository, mock(BookingRepository.class), userRepository, CLOCK);
        roleMock = Mockito.mockStatic(CurrentUserRole.class);
        idMock = Mockito.mockStatic(CurrentUserId.class);
        idMock.when(CurrentUserId::get).thenReturn(me);
        asAdmin(false);
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        roleMock.close();
        idMock.close();
    }

    private void asAdmin(boolean admin) {
        roleMock.when(() -> CurrentUserRole.has("SUPER_ADMIN")).thenReturn(admin);
    }

    private User userOf(UUID id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private TrainerSchedule scheduleOf(UUID trainerId) {
        User trainer = userOf(trainerId);
        return TrainerSchedule.builder()
                .trainer(trainer)
                .workDate(TOMORROW)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .active(true)
                .build();
    }

    /**
     * Cac ham given* dung doi tuong truoc roi moi stub repository. Khong viet
     * when(repo.x()).thenReturn(scheduleOf(...)) vi scheduleOf cung stub mock, Mockito bao UnfinishedStubbing.
     */
    private void givenShift(UUID shiftId, UUID ownerTrainerId) {
        TrainerSchedule schedule = scheduleOf(ownerTrainerId);
        when(scheduleRepository.findById(shiftId)).thenReturn(Optional.of(schedule));
    }

    private void givenUser(UUID userId) {
        User user = userOf(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private void givenShiftsInRange(UUID trainerId) {
        List<TrainerSchedule> shifts = List.of(scheduleOf(trainerId));
        when(scheduleRepository.findByTrainerIdAndWorkDateBetween(trainerId, TOMORROW, TOMORROW)).thenReturn(shifts);
    }

    private TrainerScheduleRequest.TrainerScheduleRequestBuilder request() {
        return TrainerScheduleRequest.builder()
                .workDate(TOMORROW)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0));
    }

    private static void assertForbidden(Executable action) {
        BusinessException ex = assertThrows(BusinessException.class, action);
        assertEquals(ErrorCode.AUTH_FORBIDDEN_ROLE, ex.getErrorCode());
    }

    // ---- sửa ca ----

    @Test
    void update_byAnotherTrainer_isForbiddenAndNothingSaved() {
        UUID id = UUID.randomUUID();
        givenShift(id, other);

        assertForbidden(() -> service.updateSchedule(me, id, request().build()));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void update_byOwner_saves() {
        UUID id = UUID.randomUUID();
        givenShift(id, me);

        assertNotNull(service.updateSchedule(me, id, request().build()));
        verify(scheduleRepository).save(any());
    }

    @Test
    void update_bySuperAdminOnAnotherTrainersShift_saves() {
        asAdmin(true);
        UUID id = UUID.randomUUID();
        givenShift(id, other);

        assertNotNull(service.updateSchedule(me, id, request().build()));
        verify(scheduleRepository).save(any());
    }

    // ---- xoá ca ----

    @Test
    void delete_byAnotherTrainer_isForbiddenAndNothingDeleted() {
        UUID id = UUID.randomUUID();
        givenShift(id, other);

        assertForbidden(() -> service.deleteSchedule(me, id));
        verify(scheduleRepository, never()).delete(any());
    }

    @Test
    void delete_byOwner_deletes() {
        UUID id = UUID.randomUUID();
        givenShift(id, me);

        service.deleteSchedule(me, id);
        verify(scheduleRepository).delete(any());
    }

    @Test
    void delete_bySuperAdminOnAnotherTrainersShift_deletes() {
        asAdmin(true);
        UUID id = UUID.randomUUID();
        givenShift(id, other);

        service.deleteSchedule(me, id);
        verify(scheduleRepository).delete(any());
    }

    // ---- tạo ca ----

    @Test
    void create_forAnotherTrainerWithoutAdmin_isForbiddenAndNothingSaved() {
        assertForbidden(() -> service.createSchedule(me, request().trainerId(other).build()));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void create_withoutTrainerId_createsForCurrentUser() {
        givenUser(me);

        assertNotNull(service.createSchedule(me, request().build()));
        verify(scheduleRepository).save(any());
    }

    @Test
    void create_forAnotherTrainerAsSuperAdmin_saves() {
        asAdmin(true);
        givenUser(other);

        assertNotNull(service.createSchedule(me, request().trainerId(other).build()));
        verify(scheduleRepository).save(any());
    }

    // ---- xem lịch ----

    @Test
    void list_ofAnotherTrainerWithoutAdmin_isForbidden() {
        assertForbidden(() -> service.listByTrainer(other, TOMORROW, TOMORROW));
        verify(scheduleRepository, never()).findByTrainerIdAndWorkDateBetween(any(), any(), any());
    }

    @Test
    void list_ofOwnShifts_returnsThem() {
        givenShiftsInRange(me);

        assertEquals(1, service.listByTrainer(me, TOMORROW, TOMORROW).size());
    }

    @Test
    void list_ofAnotherTrainerAsSuperAdmin_returnsThem() {
        asAdmin(true);
        givenShiftsInRange(other);

        assertEquals(1, service.listByTrainer(other, TOMORROW, TOMORROW).size());
    }
}

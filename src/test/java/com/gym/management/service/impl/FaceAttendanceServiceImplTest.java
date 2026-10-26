package com.gym.management.service.impl;

import com.gym.management.dto.request.FaceDescriptorRequest;
import com.gym.management.dto.response.BookingResponse;
import com.gym.management.dto.response.FaceAttendanceResponse;
import com.gym.management.dto.response.StaffAttendanceResponse;
import com.gym.management.entity.Role;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.integration.hardware.FaceMatchService;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.BookingService;
import com.gym.management.dto.response.TrainerScanResult;
import com.gym.management.service.StaffAttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import com.gym.management.service.GymCheckInService;


import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionTemplate được thay bằng bản giả chỉ chạy thẳng callback (không cần DataSource
 * thật) — test này chỉ kiểm tra logic routing/dedup của tầng service, không kiểm tra transaction
 * thật (thuộc phạm vi integration test, ngoài scope Agent 4 v1).
 */
@ExtendWith(MockitoExtension.class)
class FaceAttendanceServiceImplTest {

    @Mock private FaceMatchService faceMatchService;
    @Mock private UserRepository userRepository;
    @Mock private BookingService bookingService;
    @Mock private StaffAttendanceService staffAttendanceService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private GymCheckInService gymCheckInService;

    private FaceAttendanceServiceImpl service;

    private static final List<Double> DESCRIPTOR = buildDescriptor();

    private static class PassThroughTransactionTemplate extends TransactionTemplate {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(null);
        }
    }

    @BeforeEach
    void setUp() {
        service = new FaceAttendanceServiceImpl(
                faceMatchService, userRepository, bookingService, staffAttendanceService,
                redisTemplate, new PassThroughTransactionTemplate(),
                Clock.fixed(Instant.parse("2026-10-25T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")),
                gymCheckInService);
        ReflectionTestUtils.setField(service, "minScanIntervalSeconds", 120L);
        ReflectionTestUtils.setField(service, "trainerMinScanIntervalSeconds", 15L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(gymCheckInService.recordFaceScan(any(), any()))
                .thenReturn(new GymCheckInService.ScanResult("CHECK_IN", UUID.randomUUID()));
    }

    private static List<Double> buildDescriptor() {
        Double[] arr = new Double[128];
        for (int i = 0; i < 128; i++) arr[i] = 0.1;
        return List.of(arr);
    }

    private static FaceDescriptorRequest buildRequest() {
        FaceDescriptorRequest request = new FaceDescriptorRequest();
        request.setDescriptor(DESCRIPTOR);
        return request;
    }

    private User buildUser(String roleName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Test User");
        Role role = new Role();
        role.setName(roleName);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    /** Dựng sẵn mock cho luồng quét qua kiosk của 1 user đã nhận diện được, Redis cho phép đi tiếp. */
    private void givenKioskMatchedAndNotDuplicate(User user) {
        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.of(user.getId()));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
    }

    // ------------------------------------------------------------------ chung

    @Test
    void kioskScan_notRecognized_throwsFaceNotRecognized() {
        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.kioskScan(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_NOT_RECOGNIZED);

        verifyNoInteractions(userRepository, bookingService, staffAttendanceService, redisTemplate);
    }

    @Test
    void selfScan_notMatched_throwsFaceNotRecognized() {
        UUID currentUserId = UUID.randomUUID();
        when(faceMatchService.matchAgainstUser(DESCRIPTOR, currentUserId)).thenReturn(false);

        assertThatThrownBy(() -> service.selfScan(currentUserId, buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_NOT_RECOGNIZED);
    }

    @Test
    void duplicateScanWithinInterval_returnsDuplicateWithoutWriting() {
        User user = buildUser("MEMBER");

        when(faceMatchService.matchAgainstUser(DESCRIPTOR, user.getId())).thenReturn(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(valueOperations.setIfAbsent(eq("face-scan:" + user.getId()), eq("1"), any()))
                .thenReturn(false); // Redis: key đã tồn tại -> quét đúp

        FaceAttendanceResponse response = service.selfScan(user.getId(), buildRequest());

        assertThat(response.isDuplicateScan()).isTrue();
        assertThat(response.getAction()).isEqualTo("NONE");
        verifyNoInteractions(bookingService, staffAttendanceService);
    }

    @Test
    void unsupportedRole_throwsFaceNotRecognized() {
        User admin = buildUser("SUPER_ADMIN"); // role không điểm danh khuôn mặt (SALES nay đã được hỗ trợ)
        givenKioskMatchedAndNotDuplicate(admin);

        assertThatThrownBy(() -> service.kioskScan(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_NOT_RECOGNIZED);
    }

    // ----------------------------------------------------------------- MEMBER

    @Test
    void memberScan_firstScan_recordsCheckInViaBookingService() {
        User member = buildUser("MEMBER");
        BookingResponse bookingResponse = mock(BookingResponse.class);
        when(bookingResponse.getCheckOutTime()).thenReturn(null); // vừa CHECK_IN

        givenKioskMatchedAndNotDuplicate(member);
        when(bookingService.recordFaceScan(eq(member.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(bookingResponse));

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_IN");
        assertThat(response.getTargetType()).isEqualTo("BOOKING");
        assertThat(response.isBookingLinked()).isTrue();
        assertThat(response.isDuplicateScan()).isFalse();
        verifyNoInteractions(staffAttendanceService);
    }

    @Test
    void memberScan_secondScan_recordsCheckOut() {
        User member = buildUser("MEMBER");
        BookingResponse bookingResponse = mock(BookingResponse.class);
        when(bookingResponse.getCheckOutTime()).thenReturn(LocalDateTime.now()); // vừa CHECK_OUT

        givenKioskMatchedAndNotDuplicate(member);
        when(bookingService.recordFaceScan(eq(member.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(bookingResponse));

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_OUT");
    }

    @Test
    void memberScan_noMatchingBooking_recordsGymCheckIn() {
        User member = buildUser("MEMBER");

        givenKioskMatchedAndNotDuplicate(member);
        when(bookingService.recordFaceScan(eq(member.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.empty()); // không có booking hôm nay -> KHÔNG phải lỗi

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.isBookingLinked()).isFalse();
        assertThat(response.getAction()).isEqualTo("CHECK_IN");
        assertThat(response.getTargetType()).isEqualTo("GYM_CHECK_IN");
        verify(gymCheckInService).recordFaceScan(eq(member.getId()), any(LocalDateTime.class));
    }

    @Test
    void memberScan_withMatchingBooking_doesNotCreateGymCheckIn() {
        User member = buildUser("MEMBER");
        BookingResponse bookingResponse = mock(BookingResponse.class);

        givenKioskMatchedAndNotDuplicate(member);
        when(bookingService.recordFaceScan(eq(member.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(bookingResponse));

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.isBookingLinked()).isTrue();
        assertThat(response.getTargetType()).isEqualTo("BOOKING");
        verifyNoInteractions(gymCheckInService);
    }

    @Test
    void memberScan_usesDefaultDedupWindow() {
        User member = buildUser("MEMBER");
        givenKioskMatchedAndNotDuplicate(member);
        when(bookingService.recordFaceScan(eq(member.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        service.kioskScan(buildRequest());

        verify(valueOperations).setIfAbsent(
                eq("face-scan:" + member.getId()), eq("1"), eq(Duration.ofSeconds(120)));
    }

    // --------------------------------------------------------------- TRAINER

    @Test
    void trainerScan_checkIn_recordsViaBookingServiceNotStaffAttendance() {
        User trainer = buildUser("TRAINER");
        UUID bookingId = UUID.randomUUID();
        BookingResponse booking = mock(BookingResponse.class);
        when(booking.getId()).thenReturn(bookingId);

        givenKioskMatchedAndNotDuplicate(trainer);
        when(bookingService.recordTrainerFaceScan(eq(trainer.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new TrainerScanResult(booking, "CHECK_IN", false)));

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_IN");
        assertThat(response.getTargetType()).isEqualTo("BOOKING");
        assertThat(response.getBookingId()).isEqualTo(bookingId);
        assertThat(response.isBookingLinked()).isTrue();
        assertThat(response.isEarlyLeave()).isFalse();
        assertThat(response.isReasonRequired()).isFalse();
        // G3: PT quét mặt KHÔNG còn ghi staff_attendances
        verifyNoInteractions(staffAttendanceService);
        verify(bookingService, never()).recordFaceScan(any(), any());
    }

    @Test
    void trainerScan_earlyCheckOut_requiresReason() {
        User trainer = buildUser("TRAINER");
        BookingResponse booking = mock(BookingResponse.class);
        when(booking.getId()).thenReturn(UUID.randomUUID());

        givenKioskMatchedAndNotDuplicate(trainer);
        when(bookingService.recordTrainerFaceScan(eq(trainer.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new TrainerScanResult(booking, "CHECK_OUT", true)));

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_OUT");
        assertThat(response.isEarlyLeave()).isTrue();
        assertThat(response.isReasonRequired()).isTrue();
        verifyNoInteractions(staffAttendanceService);
    }

    @Test
    void trainerScan_fullDurationCheckOut_noReasonRequired() {
        User trainer = buildUser("TRAINER");
        BookingResponse booking = mock(BookingResponse.class);
        when(booking.getId()).thenReturn(UUID.randomUUID());

        givenKioskMatchedAndNotDuplicate(trainer);
        when(bookingService.recordTrainerFaceScan(eq(trainer.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new TrainerScanResult(booking, "CHECK_OUT", false)));

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_OUT");
        assertThat(response.isEarlyLeave()).isFalse();
        assertThat(response.isReasonRequired()).isFalse();
    }

    @Test
    void trainerScan_earlyLeaveFlagIgnoredOnCheckIn() {
        // Phòng thủ: dù Booking trả earlyLeave=true cho CHECK_IN (không hợp lệ), Agent 4 không hiện ô lý do
        User trainer = buildUser("TRAINER");
        BookingResponse booking = mock(BookingResponse.class);
        when(booking.getId()).thenReturn(UUID.randomUUID());

        givenKioskMatchedAndNotDuplicate(trainer);
        when(bookingService.recordTrainerFaceScan(eq(trainer.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new TrainerScanResult(booking, "CHECK_IN", true)));

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.isEarlyLeave()).isFalse();
        assertThat(response.isReasonRequired()).isFalse();
    }

    @Test
    void trainerScan_noSessionToday_returnsMessageNotError() {
        User trainer = buildUser("TRAINER");

        givenKioskMatchedAndNotDuplicate(trainer);
        when(bookingService.recordTrainerFaceScan(eq(trainer.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        FaceAttendanceResponse response = service.kioskScan(buildRequest()); // KHÔNG ném exception

        assertThat(response.getAction()).isEqualTo("NONE");
        assertThat(response.isBookingLinked()).isFalse();
        assertThat(response.getBookingId()).isNull();
        assertThat(response.getMessage()).isEqualTo("Không có buổi nào cần điểm danh");
        verifyNoInteractions(staffAttendanceService);
    }

    @Test
    void trainerScan_usesShorterDedupWindowSoBackToBackSessionsAreNotBlocked() {
        User trainer = buildUser("TRAINER");
        givenKioskMatchedAndNotDuplicate(trainer);
        when(bookingService.recordTrainerFaceScan(eq(trainer.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        service.kioskScan(buildRequest());

        verify(valueOperations).setIfAbsent(
                eq("face-scan:" + trainer.getId()), eq("1"), eq(Duration.ofSeconds(15)));
    }

    @Test
    void trainerScan_duplicateWithinShortWindow_isStillBlocked() {
        User trainer = buildUser("TRAINER");

        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.of(trainer.getId()));
        when(userRepository.findById(trainer.getId())).thenReturn(Optional.of(trainer));
        when(valueOperations.setIfAbsent(eq("face-scan:" + trainer.getId()), eq("1"), any()))
                .thenReturn(false);

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.isDuplicateScan()).isTrue();
        verifyNoInteractions(bookingService, staffAttendanceService);
    }

    // ----------------------------------------------------------- RECEPTIONIST

    @Test
    void receptionistScan_recordsViaStaffAttendanceService() {
        User receptionist = buildUser("RECEPTIONIST");
        StaffAttendanceResponse staffResponse = mock(StaffAttendanceResponse.class);
        when(staffResponse.getCheckOutTime()).thenReturn(null);

        givenKioskMatchedAndNotDuplicate(receptionist);
        when(staffAttendanceService.recordScan(eq(receptionist.getId()), any(LocalDateTime.class), eq("FACE")))
                .thenReturn(staffResponse);

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_IN");
        assertThat(response.getTargetType()).isEqualTo("STAFF_ATTENDANCE");
        verifyNoInteractions(bookingService);
    }

    // ------------------------------------------------------------------ SALES
    // Phi chốt (2026-10): Sales có chấm công khuôn mặt giống Lễ tân → ghi staff_attendances.

    @Test
    void salesScan_kiosk_recordsViaStaffAttendanceService() {
        User sales = buildUser("SALES");
        StaffAttendanceResponse staffResponse = mock(StaffAttendanceResponse.class);
        when(staffResponse.getCheckOutTime()).thenReturn(null);

        givenKioskMatchedAndNotDuplicate(sales);
        when(staffAttendanceService.recordScan(eq(sales.getId()), any(LocalDateTime.class), eq("FACE")))
                .thenReturn(staffResponse);

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_IN");
        assertThat(response.getTargetType()).isEqualTo("STAFF_ATTENDANCE");
        verifyNoInteractions(bookingService);
    }

    @Test
    void salesScan_self_secondScanIsCheckOut() {
        User sales = buildUser("SALES");
        StaffAttendanceResponse staffResponse = mock(StaffAttendanceResponse.class);
        when(staffResponse.getCheckOutTime()).thenReturn(LocalDateTime.now()); // vừa CHECK_OUT

        when(faceMatchService.matchAgainstUser(DESCRIPTOR, sales.getId())).thenReturn(true);
        when(userRepository.findById(sales.getId())).thenReturn(Optional.of(sales));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(staffAttendanceService.recordScan(eq(sales.getId()), any(LocalDateTime.class), eq("FACE")))
                .thenReturn(staffResponse);

        FaceAttendanceResponse response = service.selfScan(sales.getId(), buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_OUT");
        assertThat(response.getTargetType()).isEqualTo("STAFF_ATTENDANCE");
        verifyNoInteractions(bookingService);
    }

    @Test
    void salesScan_usesDefaultDedupWindowNotTrainerWindow() {
        User sales = buildUser("SALES");
        StaffAttendanceResponse staffResponse = mock(StaffAttendanceResponse.class);
        when(staffResponse.getCheckOutTime()).thenReturn(null);

        givenKioskMatchedAndNotDuplicate(sales);
        when(staffAttendanceService.recordScan(eq(sales.getId()), any(LocalDateTime.class), eq("FACE")))
                .thenReturn(staffResponse);

        service.kioskScan(buildRequest());

        verify(valueOperations).setIfAbsent(
                eq("face-scan:" + sales.getId()), eq("1"), eq(Duration.ofSeconds(120)));
    }

    @Test
    void receptionistScan_self_recordsViaStaffAttendanceService() {
        User receptionist = buildUser("RECEPTIONIST");
        StaffAttendanceResponse staffResponse = mock(StaffAttendanceResponse.class);
        when(staffResponse.getCheckOutTime()).thenReturn(null);

        when(faceMatchService.matchAgainstUser(DESCRIPTOR, receptionist.getId())).thenReturn(true);
        when(userRepository.findById(receptionist.getId())).thenReturn(Optional.of(receptionist));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(staffAttendanceService.recordScan(eq(receptionist.getId()), any(LocalDateTime.class), eq("FACE")))
                .thenReturn(staffResponse);

        FaceAttendanceResponse response = service.selfScan(receptionist.getId(), buildRequest());

        assertThat(response.getTargetType()).isEqualTo("STAFF_ATTENDANCE");
        verifyNoInteractions(bookingService);
    }
}

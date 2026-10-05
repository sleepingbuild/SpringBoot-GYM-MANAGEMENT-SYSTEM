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
                redisTemplate, new PassThroughTransactionTemplate());
        ReflectionTestUtils.setField(service, "minScanIntervalSeconds", 120L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
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
    void memberScan_firstScan_recordsCheckInViaBookingService() {
        User member = buildUser("MEMBER");
        BookingResponse bookingResponse = mock(BookingResponse.class);
        when(bookingResponse.getCheckOutTime()).thenReturn(null); // vừa CHECK_IN

        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.of(member.getId()));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
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

        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.of(member.getId()));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(bookingService.recordFaceScan(eq(member.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(bookingResponse));

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_OUT");
    }

    @Test
    void memberScan_noMatchingBookingToday_returnsBookingLinkedFalse() {
        User member = buildUser("MEMBER");

        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.of(member.getId()));
        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(bookingService.recordFaceScan(eq(member.getId()), any(LocalDateTime.class)))
                .thenReturn(Optional.empty()); // không có booking hôm nay -> KHÔNG phải lỗi

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.isBookingLinked()).isFalse();
    }

    @Test
    void staffScan_trainer_recordsViaStaffAttendanceService() {
        User trainer = buildUser("TRAINER");
        StaffAttendanceResponse staffResponse = mock(StaffAttendanceResponse.class);
        when(staffResponse.getCheckOutTime()).thenReturn(null);

        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.of(trainer.getId()));
        when(userRepository.findById(trainer.getId())).thenReturn(Optional.of(trainer));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(staffAttendanceService.recordScan(eq(trainer.getId()), any(LocalDateTime.class), eq("FACE")))
                .thenReturn(staffResponse);

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getAction()).isEqualTo("CHECK_IN");
        assertThat(response.getTargetType()).isEqualTo("STAFF_ATTENDANCE");
        verifyNoInteractions(bookingService);
    }

    @Test
    void staffScan_receptionist_recordsViaStaffAttendanceService() {
        User receptionist = buildUser("RECEPTIONIST");
        StaffAttendanceResponse staffResponse = mock(StaffAttendanceResponse.class);
        when(staffResponse.getCheckOutTime()).thenReturn(null);

        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.of(receptionist.getId()));
        when(userRepository.findById(receptionist.getId())).thenReturn(Optional.of(receptionist));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(staffAttendanceService.recordScan(eq(receptionist.getId()), any(LocalDateTime.class), eq("FACE")))
                .thenReturn(staffResponse);

        FaceAttendanceResponse response = service.kioskScan(buildRequest());

        assertThat(response.getTargetType()).isEqualTo("STAFF_ATTENDANCE");
    }

    @Test
    void unsupportedRole_throwsFaceNotRecognized() {
        User salesUser = buildUser("SALES"); // role không hỗ trợ điểm danh khuôn mặt

        when(faceMatchService.matchAgainstAll(DESCRIPTOR)).thenReturn(Optional.of(salesUser.getId()));
        when(userRepository.findById(salesUser.getId())).thenReturn(Optional.of(salesUser));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);

        assertThatThrownBy(() -> service.kioskScan(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACE_NOT_RECOGNIZED);
    }
}

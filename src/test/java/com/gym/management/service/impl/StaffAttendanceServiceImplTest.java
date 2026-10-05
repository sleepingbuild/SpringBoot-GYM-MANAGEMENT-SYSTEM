package com.gym.management.service.impl;

import com.gym.management.dto.response.StaffAttendanceResponse;
import com.gym.management.entity.StaffAttendance;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.StaffAttendanceRepository;
import com.gym.management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffAttendanceServiceImplTest {

    @Mock private StaffAttendanceRepository staffAttendanceRepository;
    @Mock private UserRepository userRepository;

    private StaffAttendanceServiceImpl service;

    private final UUID staffId = UUID.randomUUID();
    private User staffUser;

    @BeforeEach
    void setUp() {
        service = new StaffAttendanceServiceImpl(staffAttendanceRepository, userRepository);
        // Ca mặc định 07:00-21:00 như application-local.yml quy định (quyết định A.2)
        ReflectionTestUtils.setField(service, "defaultShiftStart", "07:00");
        ReflectionTestUtils.setField(service, "defaultShiftEnd", "21:00");

        staffUser = new User();
        staffUser.setId(staffId);
        staffUser.setFullName("PT Test");
        lenient().when(userRepository.getReferenceById(staffId)).thenReturn(staffUser);
        lenient().when(staffAttendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void recordScan_firstScanOfDay_setsCheckInAndStatusLate() {
        LocalDate today = LocalDate.now();
        LocalDateTime scanTime = today.atTime(7, 30); // trễ so với ca 07:00
        when(staffAttendanceRepository.findByStaffIdAndDate(staffId, today)).thenReturn(Optional.empty());

        StaffAttendanceResponse response = service.recordScan(staffId, scanTime, "FACE");

        assertThat(response.getCheckInTime()).isEqualTo(scanTime);
        assertThat(response.getCheckOutTime()).isNull();
        assertThat(response.getComputedStatus()).isEqualTo("LATE");
        assertThat(response.getMethod()).isEqualTo("FACE");
    }

    @Test
    void recordScan_onTimeCheckIn_statusOnTime() {
        LocalDate today = LocalDate.now();
        LocalDateTime scanTime = today.atTime(6, 55); // trước 07:00
        when(staffAttendanceRepository.findByStaffIdAndDate(staffId, today)).thenReturn(Optional.empty());

        StaffAttendanceResponse response = service.recordScan(staffId, scanTime, "FACE");

        assertThat(response.getComputedStatus()).isEqualTo("ON_TIME");
    }

    @Test
    void recordScan_secondScanOfDay_setsCheckOutAndStatusLeftEarly() {
        LocalDate today = LocalDate.now();
        StaffAttendance existing = new StaffAttendance();
        existing.setStaff(staffUser);
        existing.setDate(today);
        existing.setCheckInTime(today.atTime(6, 55)); // đúng giờ

        LocalDateTime checkOutTime = today.atTime(20, 30); // về sớm so với 21:00
        when(staffAttendanceRepository.findByStaffIdAndDate(staffId, today)).thenReturn(Optional.of(existing));

        StaffAttendanceResponse response = service.recordScan(staffId, checkOutTime, "FACE");

        assertThat(response.getCheckOutTime()).isEqualTo(checkOutTime);
        assertThat(response.getComputedStatus()).isEqualTo("LEFT_EARLY");
    }

    @Test
    void recordScan_lateCheckInAndLeftEarly_statusLateAndLeftEarly() {
        LocalDate today = LocalDate.now();
        StaffAttendance existing = new StaffAttendance();
        existing.setStaff(staffUser);
        existing.setDate(today);
        existing.setCheckInTime(today.atTime(8, 0)); // trễ

        when(staffAttendanceRepository.findByStaffIdAndDate(staffId, today)).thenReturn(Optional.of(existing));

        StaffAttendanceResponse response = service.recordScan(staffId, today.atTime(20, 0), "FACE"); // về sớm

        assertThat(response.getComputedStatus()).isEqualTo("LATE_AND_LEFT_EARLY");
    }

    @Test
    void recordScan_thirdScanOfDay_throwsValidationError() {
        LocalDate today = LocalDate.now();
        StaffAttendance existing = new StaffAttendance();
        existing.setStaff(staffUser);
        existing.setDate(today);
        existing.setCheckInTime(today.atTime(7, 0));
        existing.setCheckOutTime(today.atTime(21, 0));

        when(staffAttendanceRepository.findByStaffIdAndDate(staffId, today)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.recordScan(staffId, today.atTime(22, 0), "FACE"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void findMyAttendance_noCheckIn_returnsAbsent() {
        StaffAttendance a = new StaffAttendance();
        a.setStaff(staffUser);
        a.setDate(LocalDate.now());
        // checkInTime để null -> ABSENT

        when(staffAttendanceRepository.findByStaffIdOrderByDateDesc(staffId)).thenReturn(List.of(a));

        List<StaffAttendanceResponse> result = service.findMyAttendance(staffId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComputedStatus()).isEqualTo("ABSENT");
    }

    @Test
    void findByDate_returnsAllStaffForThatDate() {
        LocalDate date = LocalDate.now();
        StaffAttendance a = new StaffAttendance();
        a.setStaff(staffUser);
        a.setDate(date);
        a.setCheckInTime(date.atTime(7, 0));

        when(staffAttendanceRepository.findAllByDate(date)).thenReturn(List.of(a));

        List<StaffAttendanceResponse> result = service.findByDate(date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStaffId()).isEqualTo(staffId);
    }
}

package com.gym.management.service.impl;

import com.gym.management.dto.response.StaffAttendanceResponse;
import com.gym.management.entity.StaffAttendance;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.StaffAttendanceRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.StaffAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private final StaffAttendanceRepository staffAttendanceRepository;
    private final UserRepository userRepository;

    // Ca mặc định TOÀN HỆ THỐNG (quyết định A.2, giữ nguyên cho v1) — chưa phân biệt theo
    // từng nhân sự. Cải tiến sau (không bắt buộc): đọc ca thật từ trainer_schedules qua 1
    // port do Agent 3 cung cấp, hiển thị NOT_SCHEDULED khi không có ca hôm đó.
    @Value("${gms.face-attendance.default-shift-start:07:00}")
    private String defaultShiftStart;

    @Value("${gms.face-attendance.default-shift-end:21:00}")
    private String defaultShiftEnd;

    @Override
    @Transactional
    public StaffAttendanceResponse recordScan(UUID staffId, LocalDateTime scanTime, String method) {
        LocalDate today = scanTime.toLocalDate();

        StaffAttendance attendance = staffAttendanceRepository.findByStaffIdAndDate(staffId, today)
                .orElseGet(() -> {
                    StaffAttendance a = new StaffAttendance();
                    a.setStaff(userRepository.getReferenceById(staffId));
                    a.setDate(today);
                    return a;
                });

        // Lần đầu = check-in, lần 2 = check-out (nhất quán với REQUIREMENTS.md mục 4)
        if (attendance.getCheckInTime() == null) {
            attendance.setCheckInTime(scanTime);
        } else if (attendance.getCheckOutTime() == null) {
            attendance.setCheckOutTime(scanTime);
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Đã chấm công đủ 2 lượt (vào/ra) trong ngày hôm nay");
        }
        attendance.setMethod(method);

        return toResponse(staffAttendanceRepository.save(attendance));
    }

    @Override
    public List<StaffAttendanceResponse> findMyAttendance(UUID staffId) {
        return staffAttendanceRepository.findByStaffIdOrderByDateDesc(staffId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<StaffAttendanceResponse> findByDate(LocalDate date) {
        return staffAttendanceRepository.findAllByDate(date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private StaffAttendanceResponse toResponse(StaffAttendance a) {
        return StaffAttendanceResponse.builder()
                .id(a.getId())
                .staffId(a.getStaff().getId())
                .staffName(a.getStaff().getFullName())
                .date(a.getDate())
                .checkInTime(a.getCheckInTime())
                .checkOutTime(a.getCheckOutTime())
                .method(a.getMethod())
                .computedStatus(computeStatus(a))
                .notes(a.getNotes())
                .build();
    }

    private String computeStatus(StaffAttendance a) {
        if (a.getCheckInTime() == null) return "ABSENT";

        LocalTime shiftStart = LocalTime.parse(defaultShiftStart);
        LocalTime shiftEnd = LocalTime.parse(defaultShiftEnd);

        boolean late = a.getCheckInTime().toLocalTime().isAfter(shiftStart);
        // "Về sớm" chỉ xác định được SAU khi đã check-out — REQUIREMENTS.md mục 5
        if (a.getCheckOutTime() == null) {
            return late ? "LATE" : "ON_TIME";
        }
        boolean leftEarly = a.getCheckOutTime().toLocalTime().isBefore(shiftEnd);

        if (late && leftEarly) return "LATE_AND_LEFT_EARLY";
        if (late) return "LATE";
        if (leftEarly) return "LEFT_EARLY";
        return "ON_TIME";
    }
}

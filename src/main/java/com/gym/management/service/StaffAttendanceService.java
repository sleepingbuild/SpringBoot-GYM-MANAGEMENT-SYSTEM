package com.gym.management.service;

import com.gym.management.dto.response.StaffAttendanceResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StaffAttendanceService {

    /** Ghi nhận scan (FACE) hoặc chấm công thủ công (MANUAL) — lần 1 = in, lần 2 = out */
    StaffAttendanceResponse recordScan(UUID staffId, LocalDateTime scanTime, String method);

    List<StaffAttendanceResponse> findMyAttendance(UUID staffId);

    List<StaffAttendanceResponse> findByDate(LocalDate date);
}

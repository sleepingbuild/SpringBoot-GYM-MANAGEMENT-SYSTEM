package com.gym.management.controller;

import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.StaffAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/staff-attendance")
@RequiredArgsConstructor
public class StaffAttendanceController {

    private final StaffAttendanceService staffAttendanceService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('TRAINER', 'RECEPTIONIST')")
    public ApiResponse<?> myAttendance() {
        return ApiResponse.success(staffAttendanceService.findMyAttendance(CurrentUserId.get()));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<?> byDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(staffAttendanceService.findByDate(date));
    }
}

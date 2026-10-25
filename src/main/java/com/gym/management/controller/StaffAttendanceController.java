package com.gym.management.controller;

import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.StaffAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Staff Attendance", description = "Chấm công nhân sự (Lễ tân, Sales). PT điểm danh theo buổi ở Booking, không ở đây")
@RestController
@RequestMapping("/api/v1/staff-attendance")
@RequiredArgsConstructor
public class StaffAttendanceController {

    private final StaffAttendanceService staffAttendanceService;

    // G3 (CHANGE_PT_ATTENDANCE_AND_PAY.md): PT không còn ghi staff_attendances (trả lương theo
    // buổi). Phi chốt Sales có chấm công khuôn mặt → Lễ tân và Sales xem được lịch sử của mình.
    @Operation(summary = "Xem lịch sử chấm công của chính mình (kèm trạng thái đúng giờ/đi muộn/về sớm)")
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SALES')")
    public ApiResponse<?> myAttendance() {
        return ApiResponse.success(staffAttendanceService.findMyAttendance(CurrentUserId.get()));
    }

    @Operation(summary = "Báo cáo chấm công toàn bộ nhân sự theo ngày")
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<?> byDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(staffAttendanceService.findByDate(date));
    }
}

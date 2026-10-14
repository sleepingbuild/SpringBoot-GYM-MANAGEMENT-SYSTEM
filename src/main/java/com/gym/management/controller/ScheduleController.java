package com.gym.management.controller;

import com.gym.management.dto.request.TrainerScheduleRequest;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.TrainerScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/** Ca làm việc của PT theo ngày cụ thể (work_date). Quyền theo role, không có tiền tố ROLE_ trong tên role. */
@Tag(name = "Lịch làm việc PT")
@RestController
@RequestMapping("/api/v1/trainer-schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final TrainerScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TRAINER', 'SUPER_ADMIN')")
    @Operation(summary = "Tạo ca làm việc của PT (theo ngày cụ thể)")
    public ApiResponse<?> create(@Valid @RequestBody TrainerScheduleRequest request) {
        UUID currentUserId = CurrentUserId.get();
        return ApiResponse.success("Tạo ca làm việc thành công", scheduleService.createSchedule(currentUserId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER', 'SUPER_ADMIN')")
    @Operation(summary = "Sửa ca làm việc của PT")
    public ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody TrainerScheduleRequest request) {
        UUID currentUserId = CurrentUserId.get();
        return ApiResponse.success("Cập nhật ca làm việc thành công", scheduleService.updateSchedule(currentUserId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xoá ca làm việc của PT")
    public void delete(@PathVariable UUID id) {
        scheduleService.deleteSchedule(CurrentUserId.get(), id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRAINER', 'SUPER_ADMIN')")
    @Operation(summary = "Danh sách ca làm việc của PT")
    public ApiResponse<?> listByTrainer(
            @RequestParam(required = false) UUID trainerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID targetTrainer = trainerId != null ? trainerId : CurrentUserId.get();
        return ApiResponse.success(scheduleService.listByTrainer(targetTrainer, from, to));
    }

    @GetMapping("/available")
    @Operation(summary = "Slot trống của PT trong 1 ngày")
    public ApiResponse<?> available(
            @RequestParam UUID trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(scheduleService.findAvailableSlots(trainerId, date));
    }
}

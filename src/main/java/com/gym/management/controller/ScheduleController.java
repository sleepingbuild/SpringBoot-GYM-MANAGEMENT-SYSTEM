package com.gym.management.controller;

import com.gym.management.dto.request.TrainerScheduleRequest;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.TrainerScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ⚠️ @PreAuthorize giả định role authorities có prefix "ROLE_" theo chuẩn Spring Security
 * (khớp cách Agent 2 dùng — xem AGENT2_README.md). Nếu SecurityConfig của Agent 1 dùng cơ chế
 * khác, chỉ cần đổi annotation ở đây, không đổi logic service.
 */
@RestController
@RequestMapping("/api/v1/trainer-schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final TrainerScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TRAINER', 'SUPER_ADMIN')")
    public ApiResponse<?> create(@Valid @RequestBody TrainerScheduleRequest request) {
        UUID currentUserId = CurrentUserId.get();
        return ApiResponse.success("Tạo ca làm việc thành công", scheduleService.createSchedule(currentUserId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER', 'SUPER_ADMIN')")
    public ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody TrainerScheduleRequest request) {
        UUID currentUserId = CurrentUserId.get();
        return ApiResponse.success("Cập nhật ca làm việc thành công", scheduleService.updateSchedule(currentUserId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAINER', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        scheduleService.deleteSchedule(CurrentUserId.get(), id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TRAINER', 'SUPER_ADMIN')")
    public ApiResponse<?> listByTrainer(
            @RequestParam(required = false) UUID trainerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID targetTrainer = trainerId != null ? trainerId : CurrentUserId.get();
        return ApiResponse.success(scheduleService.listByTrainer(targetTrainer, from, to));
    }

    @GetMapping("/available")
    public ApiResponse<?> available(
            @RequestParam UUID trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(scheduleService.findAvailableSlots(trainerId, date));
    }
}

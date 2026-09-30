package com.gym.management.controller;

import com.gym.management.dto.request.BookingRequest;
import com.gym.management.dto.request.BookingUpdateRequest;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.security.CurrentUserRole;
import com.gym.management.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * ⚠️ CurrentUserRole.get() là shim TẠM giống CurrentUserId (xem AGENT3_README.md mục 1) —
 * giả định JWT có claim role đơn (1 user có đúng 1 role hiệu lực tại 1 thời điểm gọi API,
 * khớp mô hình 5 role trong REQUIREMENTS.md). Nếu Agent 1 dùng Collection<Role>, đổi 1 chỗ
 * duy nhất trong CurrentUserRole, không phải sửa Controller/Service.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> create(@Valid @RequestBody BookingRequest request) {
        return ApiResponse.success("Đặt lịch thành công",
                bookingService.createBooking(CurrentUserId.get(), CurrentUserRole.get(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody BookingUpdateRequest request) {
        return ApiResponse.success("Cập nhật lịch thành công",
                bookingService.updateBooking(CurrentUserId.get(), CurrentUserRole.get(), id, request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('TRAINER')")
    public ApiResponse<?> confirm(@PathVariable UUID id) {
        return ApiResponse.success("Xác nhận buổi tập thành công",
                bookingService.confirmBooking(CurrentUserId.get(), id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('TRAINER')")
    public ApiResponse<?> complete(@PathVariable UUID id) {
        return ApiResponse.success("Đánh dấu hoàn thành buổi tập thành công",
                bookingService.completeBooking(CurrentUserId.get(), id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MEMBER', 'TRAINER', 'RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Huỷ buổi tập thành công",
                bookingService.cancelBooking(CurrentUserId.get(), CurrentUserRole.get(), id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'TRAINER')")
    public ApiResponse<?> myBookings() {
        return ApiResponse.success(bookingService.findMyBookings(CurrentUserId.get(), CurrentUserRole.get()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    public ApiResponse<?> byBranchAndDate(
            @RequestParam UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(bookingService.findByBranchAndDate(branchId, date));
    }
}

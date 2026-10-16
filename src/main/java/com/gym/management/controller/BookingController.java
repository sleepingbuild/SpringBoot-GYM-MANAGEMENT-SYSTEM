package com.gym.management.controller;

import com.gym.management.dto.request.BookingRequest;
import com.gym.management.dto.request.BookingUpdateRequest;
import com.gym.management.dto.request.EarlyLeaveReasonRequest;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.security.CurrentUserRole;
import com.gym.management.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/** Đặt lịch PT và vòng đời booking. Mỗi user có đúng 1 role hiệu lực, lấy qua CurrentUserRole.get(). */
@Tag(name = "Booking (đặt lịch PT)")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'RECEPTIONIST', 'SUPER_ADMIN')")
    @Operation(summary = "Đặt lịch PT (member tự đặt hoặc lễ tân/admin đặt hộ)")
    public ApiResponse<?> create(@Valid @RequestBody BookingRequest request) {
        return ApiResponse.success("Đặt lịch thành công",
                bookingService.createBooking(CurrentUserId.get(), CurrentUserRole.get(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'RECEPTIONIST', 'SUPER_ADMIN')")
    @Operation(summary = "Sửa lịch PT (áp lại đủ rule)")
    public ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody BookingUpdateRequest request) {
        return ApiResponse.success("Cập nhật lịch thành công",
                bookingService.updateBooking(CurrentUserId.get(), CurrentUserRole.get(), id, request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "PT xác nhận buổi tập")
    public ApiResponse<?> confirm(@PathVariable UUID id) {
        return ApiResponse.success("Xác nhận buổi tập thành công",
                bookingService.confirmBooking(CurrentUserId.get(), id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "PT hoàn thành buổi tập")
    public ApiResponse<?> complete(@PathVariable UUID id) {
        return ApiResponse.success("Đánh dấu hoàn thành buổi tập thành công",
                bookingService.completeBooking(CurrentUserId.get(), id));
    }

    @PutMapping("/{id}/early-leave-reason")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "PT nộp lý do rời buổi sớm (sau khi quét mặt ra trước giờ kết thúc)")
    public ApiResponse<?> earlyLeaveReason(@PathVariable UUID id, @Valid @RequestBody EarlyLeaveReasonRequest request) {
        return ApiResponse.success("Đã ghi nhận lý do rời buổi sớm",
                bookingService.submitEarlyLeaveReason(CurrentUserId.get(), id, request.getReason()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MEMBER', 'TRAINER', 'RECEPTIONIST', 'SUPER_ADMIN')")
    @Operation(summary = "Huỷ buổi tập (học viên huỷ khi đã CONFIRMED và còn dưới 2 giờ = huỷ muộn)")
    public ApiResponse<?> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Huỷ buổi tập thành công",
                bookingService.cancelBooking(CurrentUserId.get(), CurrentUserRole.get(), id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'TRAINER')")
    @Operation(summary = "Lịch của tôi (member hoặc PT)")
    public ApiResponse<?> myBookings() {
        return ApiResponse.success(bookingService.findMyBookings(CurrentUserId.get(), CurrentUserRole.get()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'SUPER_ADMIN')")
    @Operation(summary = "Lịch theo chi nhánh và ngày (lễ tân/admin)")
    public ApiResponse<?> byBranchAndDate(
            @RequestParam UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(bookingService.findByBranchAndDate(branchId, date));
    }
}

package com.gym.management.service;

import com.gym.management.dto.request.BookingRequest;
import com.gym.management.dto.request.BookingUpdateRequest;
import com.gym.management.dto.response.BookingResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingService {

    /**
     * @param currentUserId  người đang gọi API (member tự đặt HOẶC lễ tân/admin đặt hộ)
     * @param currentUserRole role của currentUserId — dùng để quyết định memberId lấy từ
     *                        request (đặt hộ) hay từ chính currentUserId (tự đặt)
     */
    BookingResponse createBooking(UUID currentUserId, String currentUserRole, BookingRequest request);

    BookingResponse updateBooking(UUID currentUserId, String currentUserRole, UUID bookingId, BookingUpdateRequest request);

    BookingResponse confirmBooking(UUID trainerId, UUID bookingId);

    BookingResponse completeBooking(UUID trainerId, UUID bookingId);

    BookingResponse cancelBooking(UUID currentUserId, String currentUserRole, UUID bookingId);

    BookingResponse getById(UUID bookingId);

    List<BookingResponse> findMyBookings(UUID currentUserId, String currentUserRole);

    List<BookingResponse> findByBranchAndDate(UUID branchId, LocalDate date);

    // ---- Dành cho Agent 4 (Face Attendance) gọi qua service, không đụng Repository ----

    /**
     * Ghi nhận 1 lượt quét mặt hợp lệ của MEMBER (userId đã được Agent 4 xác định chắc chắn
     * qua so khớp Euclidean — Booking KHÔNG tự verify lại danh tính).
     * Tự quyết định là check-in hay check-out theo lượt quét trong ngày:
     * lần đầu = check-in (set check_in_time), lần tiếp theo = check-out (set check_out_time).
     *
     * @return booking vừa được cập nhật, hoặc empty nếu hôm nay member không có booking nào
     *         phù hợp để điểm danh (không phải lỗi — Agent 4 tự quyết định thông báo gì cho FE).
     */
    Optional<BookingResponse> recordFaceScan(UUID memberId, LocalDateTime scanTime);
}

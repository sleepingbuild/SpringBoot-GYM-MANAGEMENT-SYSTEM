package com.gym.management.service;

import java.util.UUID;

/**
 * Cổng cho module Membership (Agent 2) hỏi số buổi hội viên đang giữ chỗ ở module Booking (Agent 3).
 * Membership chỉ phụ thuộc interface này, không đọc thẳng bảng bookings.
 *
 * Không inject cổng này vào MembershipServiceImpl: BookingServiceImpl đã phụ thuộc MembershipLookupPort
 * (do Membership triển khai) nên sẽ thành vòng bean. Dùng ở một bean ghép tầng trên.
 */
public interface OpenBookingCountPort {

    /**
     * Số booking đang mở (PENDING và CONFIRMED) của hội viên, đã áp lazy-status: PENDING quá hạn tự huỷ
     * và CONFIRMED vừa lật sang NO_SHOW/PT_NO_SHOW thì không tính. Đây đúng là con số luật đặt lịch dùng
     * (số buổi còn lại trừ số booking mở phải lớn hơn 0), nên "còn đặt được" = max(0, remaining - số này).
     *
     * Lazy-status có thể ghi DB (huỷ booking quá hạn, phát sự kiện NO_SHOW) nên hàm chạy trong transaction
     * ghi được; người gọi không được đánh dấu readOnly.
     */
    int countOpenBookings(UUID memberId);
}

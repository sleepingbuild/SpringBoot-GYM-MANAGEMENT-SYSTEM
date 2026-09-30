package com.gym.management.service;

import java.util.UUID;

/**
 * Cổng gọi sang module Membership (Agent 2) để lấy giới hạn buổi/tuần của gói ĐANG ACTIVE.
 *
 * Lý do tách interface riêng thay vì gọi thẳng `MembershipService.getCurrentMembership(...)`
 * trong BookingServiceImpl: tại thời điểm viết code này, DTO trả về thật của Agent 2
 * (`getCurrentMembership`, xem AGENT2_README.md mục 4 — Issue 2.5 đã ✅) chưa được cung cấp
 * cho Agent 3. Toàn bộ logic nghiệp vụ của Booking chỉ phụ thuộc vào 1 method duy nhất ở đây
 * (MembershipLookupPortImpl) — nếu chữ ký thật của Agent 2 khác, CHỈ cần sửa
 * MembershipLookupPortImpl, không phải sửa BookingServiceImpl.
 *
 * ⚠️ CẦN AGENT 2 XÁC NHẬN: tên method thật + tên field `maxSessionsPerWeek` trên response.
 * Xem AGENT3_README.md mục 1.
 */
public interface MembershipLookupPort {

    /**
     * @return null nếu package hiện tại KHÔNG giới hạn buổi/tuần (max_sessions_per_week = NULL
     *         trong DB); 0 nếu gói không cho đặt PT; N nếu giới hạn N buổi/tuần.
     * @throws com.gym.management.exception.BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND)
     *         nếu member không có gói nào đang ACTIVE — xác nhận: MembershipService.getCurrentMembership()
     *         trả Optional.empty() trong trường hợp này (Agent 2 xác nhận 2026-09-30), KHÔNG trả
     *         null. Impl (`MembershipLookupPortImpl`) đã unwrap bằng `.orElseThrow(...)`.
     */
    Integer resolveMaxSessionsPerWeek(UUID memberId);
}

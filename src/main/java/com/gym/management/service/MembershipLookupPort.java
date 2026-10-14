package com.gym.management.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Cổng gọi sang module Membership (Agent 2) để lấy thông tin gói ĐANG ACTIVE của hội viên.
 * Booking chỉ phụ thuộc interface này, không gọi thẳng MembershipService.
 */
public interface MembershipLookupPort {

    /**
     * Thông tin gói ACTIVE cần cho việc đặt lịch, lấy trong một lần gọi.
     *
     * @param maxSessionsPerWeek null = không giới hạn buổi/tuần; 0 = gói không cho đặt PT; N = tối đa N buổi/tuần
     * @param remainingSessions  null = gói không tính theo buổi (không giới hạn); N = số buổi còn lại
     * @param endDate            ngày CUỐI CÙNG còn hiệu lực của gói (đã bao gồm); null = không có hạn
     */
    record ActiveMembership(Integer maxSessionsPerWeek, Integer remainingSessions, LocalDate endDate) {
    }

    /**
     * @throws com.gym.management.exception.BusinessException (MEMBERSHIP_NOT_FOUND) nếu hội viên
     *         không có gói nào đang ACTIVE: phải có gói ACTIVE mới đặt được lịch PT.
     */
    ActiveMembership resolveActiveMembership(UUID memberId);
}

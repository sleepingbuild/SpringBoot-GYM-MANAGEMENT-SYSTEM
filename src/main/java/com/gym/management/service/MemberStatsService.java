package com.gym.management.service;

import com.gym.management.dto.response.MemberStatsResponse;

import java.util.UUID;

public interface MemberStatsService {

    /**
     * Thống kê cá nhân của hội viên trong 30 ngày gần nhất. Dữ liệu lấy qua MembershipService,
     * BookingService và CheckInHistoryService, không đọc thẳng bảng của module khác.
     */
    MemberStatsResponse getMyStats(UUID memberId);
}

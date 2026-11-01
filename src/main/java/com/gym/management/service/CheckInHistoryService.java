package com.gym.management.service;

import com.gym.management.dto.response.CheckInHistoryItem;

import java.util.List;
import java.util.UUID;

public interface CheckInHistoryService {

    /**
     * M17: lịch sử vào phòng tập của hội viên, mới nhất trước. Hợp của `gym_check_ins` và các
     * booking đã có lượt quét vào của hội viên (`bookings` lấy qua BookingService).
     */
    List<CheckInHistoryItem> findMyHistory(UUID memberId);
}

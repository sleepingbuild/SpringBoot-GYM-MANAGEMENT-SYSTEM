package com.gym.management.dto.response;

import java.time.LocalDate;
import java.util.List;

/** Agent 6 - Lưu lượng lượt hội viên quét mặt vào phòng tập: bookings.check_in_time (có buổi PT) và gym_check_ins (không có buổi PT). */
public record CheckinTrafficResponse(
        LocalDate from,
        LocalDate to,
        long total,
        List<DayCount> byDay,
        List<HourCount> byHour) {

    public record DayCount(LocalDate date, long count) {
    }

    /** hour: 0 đến 23. */
    public record HourCount(int hour, long count) {
    }
}

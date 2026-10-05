package com.gym.management.service;

import com.gym.management.dto.request.TrainerScheduleRequest;
import com.gym.management.dto.response.AvailableSlotResponse;
import com.gym.management.dto.response.TrainerScheduleResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TrainerScheduleService {

    TrainerScheduleResponse createSchedule(UUID currentUserId, TrainerScheduleRequest request);

    TrainerScheduleResponse updateSchedule(UUID currentUserId, UUID scheduleId, TrainerScheduleRequest request);

    void deleteSchedule(UUID currentUserId, UUID scheduleId);

    List<TrainerScheduleResponse> listByTrainer(UUID trainerId, LocalDate from, LocalDate to);

    /**
     * Trả danh sách slot trống của 1 PT trong 1 ngày, dựa trên ca làm việc thật (TrainerSchedule)
     * trừ đi các booking đã có (không cấm trùng giờ giữa 2 PT khác nhau).
     * Độ dài mỗi slot lấy theo `gms.booking.slot-duration-minutes` (mặc định 60) —
     * xem AGENT3_README.md mục 4, REQUIREMENTS.md không quy định cụ thể giá trị này.
     */
    List<AvailableSlotResponse> findAvailableSlots(UUID trainerId, LocalDate date);

    /** Dùng nội bộ bởi BookingService để kiểm tra 1 khung giờ có nằm trong ca làm việc thật không. */
    boolean isWithinWorkingHours(UUID trainerId, LocalDate date, java.time.LocalTime start, java.time.LocalTime end);
}

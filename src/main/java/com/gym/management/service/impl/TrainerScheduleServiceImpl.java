package com.gym.management.service.impl;

import com.gym.management.dto.request.TrainerScheduleRequest;
import com.gym.management.dto.response.AvailableSlotResponse;
import com.gym.management.dto.response.TrainerScheduleResponse;
import com.gym.management.entity.Booking;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.TrainerSchedule;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.BookingRepository;
import com.gym.management.repository.TrainerScheduleRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.service.TrainerScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GIẢ ĐỊNH cần xác nhận (xem AGENT3_README.md):
 * - UserRepository đã tồn tại sẵn (Agent 1 sở hữu, Agent 2 cũng dùng lại — xem AGENT2_README.md
 *   mục 1: "nhiều khả năng Agent 1 đã có sẵn"). Agent 3 KHÔNG tự tạo lại file này.
 * - Timezone hệ thống cố định Asia/Ho_Chi_Minh (REQUIREMENTS.md mục 7) — áp dụng qua bean
 *   `Clock` (xem BookingClockConfig) thay vì gọi trực tiếp `LocalDate.now()`, để test đơn vị
 *   giả lập được thời gian.
 */
@Service
@RequiredArgsConstructor
public class TrainerScheduleServiceImpl implements TrainerScheduleService {

    private final TrainerScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Value("${gms.booking.slot-duration-minutes:60}")
    private int slotDurationMinutes;

    @Override
    @Transactional
    public TrainerScheduleResponse createSchedule(UUID currentUserId, TrainerScheduleRequest request) {
        UUID trainerId = request.getTrainerId() != null ? request.getTrainerId() : currentUserId;
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy PT"));

        TrainerSchedule schedule = TrainerSchedule.builder()
                .trainer(trainer)
                .workDate(request.getWorkDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .active(request.getActive() == null || request.getActive())
                .build();

        validateScheduleTime(schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime());

        return toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public TrainerScheduleResponse updateSchedule(UUID currentUserId, UUID scheduleId, TrainerScheduleRequest request) {
        TrainerSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy ca làm việc"));

        // Gán đủ trường TRƯỚC khi validate (REQUIREMENTS.md mục 7 / mục 3 — bug "fail âm thầm" ở .NET)
        schedule.setWorkDate(request.getWorkDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        if (request.getActive() != null) {
            schedule.setActive(request.getActive());
        }

        validateScheduleTime(schedule.getWorkDate(), schedule.getStartTime(), schedule.getEndTime());

        return toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public void deleteSchedule(UUID currentUserId, UUID scheduleId) {
        TrainerSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy ca làm việc"));
        scheduleRepository.delete(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainerScheduleResponse> listByTrainer(UUID trainerId, LocalDate from, LocalDate to) {
        List<TrainerSchedule> schedules = (from != null && to != null)
                ? scheduleRepository.findByTrainerIdAndWorkDateBetween(trainerId, from, to)
                : scheduleRepository.findByTrainerIdAndWorkDate(trainerId, LocalDate.now(clock));
        return schedules.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> findAvailableSlots(UUID trainerId, LocalDate date) {
        List<TrainerSchedule> shifts = scheduleRepository.findByTrainerIdAndWorkDateAndActiveTrue(trainerId, date);
        if (shifts.isEmpty()) {
            return List.of();
        }

        // Cần TOÀN BỘ booking trong ngày của PT đó để loại slot đã bị chiếm (không tính CANCELLED).
        // findActiveByTrainerSlot trong BookingRepository chỉ check 1 slot cụ thể nên không dùng ở đây.
        List<Booking> allTrainerBookings = bookingRepository
                .findByTrainerIdOrderByBookingDateDescStartTimeDesc(trainerId).stream()
                .filter(b -> b.getBookingDate().equals(date) && b.getStatus() != BookingStatus.CANCELLED)
                .toList();

        boolean isToday = date.equals(LocalDate.now(clock));
        LocalTime now = LocalTime.now(clock);

        List<AvailableSlotResponse> result = new ArrayList<>();
        for (TrainerSchedule shift : shifts) {
            LocalTime cursor = shift.getStartTime();
            while (!cursor.plusMinutes(slotDurationMinutes).isAfter(shift.getEndTime())) {
                LocalTime slotEnd = cursor.plusMinutes(slotDurationMinutes);
                final LocalTime slotStart = cursor;

                boolean taken = allTrainerBookings.stream()
                        .anyMatch(b -> b.getStartTime().equals(slotStart));
                boolean tooSoon = isToday && !slotStart.isAfter(now.plusMinutes(30));

                if (!taken && !tooSoon) {
                    result.add(AvailableSlotResponse.builder()
                            .trainerId(trainerId)
                            .date(date)
                            .startTime(slotStart)
                            .endTime(slotEnd)
                            .build());
                }
                cursor = slotEnd;
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWithinWorkingHours(UUID trainerId, LocalDate date, LocalTime start, LocalTime end) {
        return scheduleRepository.findByTrainerIdAndWorkDateAndActiveTrue(trainerId, date).stream()
                .anyMatch(s -> !start.isBefore(s.getStartTime()) && !end.isAfter(s.getEndTime()));
    }

    private void validateScheduleTime(LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        LocalDate today = LocalDate.now(clock);
        if (workDate.isBefore(today)) {
            throw new BusinessException(ErrorCode.SCHEDULE_TIME_INVALID, "Không thể tạo ca ở ngày quá khứ");
        }
        if (workDate.isEqual(today) && startTime.isBefore(LocalTime.now(clock))) {
            throw new BusinessException(ErrorCode.SCHEDULE_TIME_INVALID,
                    "Giờ bắt đầu phải lớn hơn hoặc bằng giờ hiện tại khi tạo ca cho hôm nay");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.SCHEDULE_TIME_INVALID, "Giờ kết thúc phải sau giờ bắt đầu");
        }
    }

    private TrainerScheduleResponse toResponse(TrainerSchedule s) {
        return TrainerScheduleResponse.builder()
                .id(s.getId())
                .trainerId(s.getTrainer().getId())
                .trainerName(s.getTrainer().getFullName())
                .workDate(s.getWorkDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .active(s.isActive())
                .build();
    }
}

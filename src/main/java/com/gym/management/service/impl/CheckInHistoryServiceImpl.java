package com.gym.management.service.impl;

import com.gym.management.dto.response.BookingResponse;
import com.gym.management.dto.response.CheckInHistoryItem;
import com.gym.management.entity.GymCheckIn;
import com.gym.management.repository.GymCheckInRepository;
import com.gym.management.service.BookingService;
import com.gym.management.service.CheckInHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckInHistoryServiceImpl implements CheckInHistoryService {

    // Vai trò truyền cho BookingService.findMyBookings; endpoint đã giới hạn hasRole('MEMBER').
    private static final String ROLE_MEMBER = "MEMBER";

    private final GymCheckInRepository gymCheckInRepository;
    private final BookingService bookingService;

    @Override
    @Transactional(readOnly = true)
    public List<CheckInHistoryItem> findMyHistory(UUID memberId) {
        List<CheckInHistoryItem> items = new ArrayList<>();

        for (GymCheckIn c : gymCheckInRepository.findByMemberIdOrderByCheckInTimeDesc(memberId)) {
            items.add(CheckInHistoryItem.builder()
                    .type(CheckInHistoryItem.TYPE_GYM_VISIT)
                    .id(c.getId())
                    .checkInTime(c.getCheckInTime())
                    .checkOutTime(c.getCheckOutTime())
                    .method(c.getMethod())
                    .build());
        }

        // Chỉ lấy buổi PT mà HỘI VIÊN đã quét vào (checkInTime). ptCheckInTime là của PT.
        for (BookingResponse b : bookingService.findMyBookings(memberId, ROLE_MEMBER)) {
            if (b.getCheckInTime() == null) {
                continue;
            }
            items.add(CheckInHistoryItem.builder()
                    .type(CheckInHistoryItem.TYPE_PT_SESSION)
                    .id(b.getId())
                    .checkInTime(b.getCheckInTime())
                    .checkOutTime(b.getCheckOutTime())
                    .method(b.getCheckInMethod())
                    .trainerName(b.getTrainerName())
                    .build());
        }

        items.sort(Comparator.comparing(CheckInHistoryItem::getCheckInTime).reversed());
        return items;
    }
}

package com.gym.management.service.impl;

import com.gym.management.dto.response.BookingResponse;
import com.gym.management.dto.response.CheckInHistoryItem;
import com.gym.management.dto.response.MemberStatsResponse;
import com.gym.management.entity.BookingStatus;
import com.gym.management.entity.MemberPackage;
import com.gym.management.service.BookingService;
import com.gym.management.service.CheckInHistoryService;
import com.gym.management.service.MemberStatsService;
import com.gym.management.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberStatsServiceImpl implements MemberStatsService {

    static final int WINDOW_DAYS = 30;
    // Vai trò truyền cho BookingService.findMyBookings; endpoint đã giới hạn hasRole('MEMBER').
    private static final String ROLE_MEMBER = "MEMBER";

    private final MembershipService membershipService;
    private final BookingService bookingService;
    private final CheckInHistoryService checkInHistoryService;
    /** Bean Clock dùng chung của dự án (múi giờ Asia/Ho_Chi_Minh), để "hôm nay" khớp mọi module. */
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public MemberStatsResponse getMyStats(UUID memberId) {
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(WINDOW_DAYS - 1L);

        // Booking trong cửa sổ: đếm theo trạng thái và số buổi hoàn thành.
        List<BookingResponse> window = bookingService.findMyBookings(memberId, ROLE_MEMBER, null, from, to);
        Map<BookingStatus, Integer> counts = new EnumMap<>(BookingStatus.class);
        for (BookingStatus s : BookingStatus.values()) {
            counts.put(s, 0);
        }
        for (BookingResponse b : window) {
            counts.merge(b.getStatus(), 1, Integer::sum);
        }
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        counts.forEach((status, n) -> byStatus.put(status.name(), n));

        // Số liệu không giới hạn thời gian: tổng buổi đã hoàn thành và số buổi đang giữ chỗ.
        int completedTotal = bookingService.findMyBookings(memberId, ROLE_MEMBER, BookingStatus.COMPLETED, null, null).size();
        int reserved = bookingService.findMyBookings(memberId, ROLE_MEMBER, BookingStatus.PENDING, null, null).size()
                + bookingService.findMyBookings(memberId, ROLE_MEMBER, BookingStatus.CONFIRMED, null, null).size();

        int visits = (int) checkInHistoryService.findMyHistory(memberId).stream()
                .map(CheckInHistoryItem::getCheckInTime)
                .map(t -> t.toLocalDate())
                .filter(d -> !d.isBefore(from) && !d.isAfter(to))
                .count();

        MemberStatsResponse.MemberStatsResponseBuilder out = MemberStatsResponse.builder()
                .from(from)
                .to(to)
                .reservedSessions(reserved)
                .completedSessionsTotal(completedTotal)
                .completedSessionsLast30Days(counts.get(BookingStatus.COMPLETED))
                .gymVisitsLast30Days(visits)
                .bookingsByStatusLast30Days(byStatus);

        Optional<MemberPackage> current = membershipService.getCurrentMembership(memberId);
        if (current.isPresent()) {
            MemberPackage mp = current.get();
            Integer remaining = mp.getRemainingSessions();
            out.hasActiveMembership(true)
                    .packageName(mp.getGymPackage() == null ? null : mp.getGymPackage().getName())
                    .membershipEndDate(mp.getEndDate())
                    .remainingSessions(remaining)
                    .availableSessions(remaining == null ? null : Math.max(0, remaining - reserved));
        } else {
            out.hasActiveMembership(false);
        }
        return out.build();
    }
}

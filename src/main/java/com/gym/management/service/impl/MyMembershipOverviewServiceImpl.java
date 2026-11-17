package com.gym.management.service.impl;

import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.service.MembershipService;
import com.gym.management.service.MyMembershipOverviewService;
import com.gym.management.service.OpenBookingCountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MyMembershipOverviewServiceImpl implements MyMembershipOverviewService {

    private final MembershipService membershipService;
    private final OpenBookingCountPort openBookingCountPort;

    /**
     * KHÔNG gắn readOnly: lazy-check gói (hết hạn, SCHEDULED thành ACTIVE) và lazy-status của booking
     * (huỷ PENDING quá hạn, NO_SHOW) đều có thể ghi DB.
     */
    @Override
    @Transactional
    public MemberPackageResponse getMyCurrentMembership(UUID userId) {
        MemberPackageResponse mp = membershipService.getCurrentMembershipResponse(userId);
        Integer remaining = mp.getRemainingSessions();
        if (remaining == null) {
            return mp; // gói không tính theo buổi: không có khái niệm giữ chỗ, bỏ qua (không gọi sang Booking)
        }
        int held = openBookingCountPort.countOpenBookings(userId);
        mp.setHeldSessions(held);
        mp.setBookableSessions(Math.max(0, remaining - held));
        return mp;
    }
}

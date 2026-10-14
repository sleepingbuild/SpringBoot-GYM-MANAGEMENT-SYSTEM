package com.gym.management.service.impl;

import com.gym.management.entity.MemberPackage;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.service.MembershipLookupPort;
import com.gym.management.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Điểm duy nhất nối sang MembershipService. getCurrentMembership() trả Optional.empty() khi hội viên
 * không có gói ACTIVE; gói liên kết qua field gymPackage của MemberPackage (getGymPackage()).
 * Nếu getter của MemberPackage đổi tên thì chỉ phải sửa file này.
 */
@Component
@RequiredArgsConstructor
public class MembershipLookupPortImpl implements MembershipLookupPort {

    private final MembershipService membershipService;

    @Override
    public ActiveMembership resolveActiveMembership(UUID memberId) {
        MemberPackage current = membershipService.getCurrentMembership(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBERSHIP_NOT_FOUND,
                        "Hội viên chưa có gói tập đang hoạt động, không thể đặt lịch PT"));
        return new ActiveMembership(
                current.getGymPackage().getMaxSessionsPerWeek(),
                current.getRemainingSessions(),
                current.getEndDate());
    }
}

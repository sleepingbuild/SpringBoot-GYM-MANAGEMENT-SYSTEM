package com.gym.management.service;

import com.gym.management.dto.response.MemberPackageResponse;

import java.util.UUID;

/**
 * Agent 2 - Gói hiện tại của chính hội viên, kèm số buổi đang giữ chỗ và số buổi còn đặt được.
 *
 * Là bean ghép ở tầng trên: phụ thuộc MembershipService và OpenBookingCountPort (Agent 3). KHÔNG đưa
 * OpenBookingCountPort vào MembershipServiceImpl vì sẽ tạo vòng bean
 * (Membership -> Booking -> MembershipLookupPort -> Membership). Không bean nào được inject bean này ngoài controller.
 */
public interface MyMembershipOverviewService {

    /** Như {@link MembershipService#getCurrentMembershipResponse}, thêm heldSessions và bookableSessions. */
    MemberPackageResponse getMyCurrentMembership(UUID userId);
}

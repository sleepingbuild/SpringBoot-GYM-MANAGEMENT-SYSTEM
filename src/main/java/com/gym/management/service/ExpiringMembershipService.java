package com.gym.management.service;

import com.gym.management.dto.response.ExpiringMemberResponse;

import java.util.List;

/** Agent 6 - Workflow hội viên sắp hết hạn (Issue 6.3). */
public interface ExpiringMembershipService {

    int DEFAULT_DAYS = 7;
    int MAX_DAYS = 90;

    /** Hội viên có gói ACTIVE hết hạn từ hôm nay đến hôm nay cộng days ngày; days từ 1 đến 90. */
    List<ExpiringMemberResponse> findExpiringSoon(int days);
}

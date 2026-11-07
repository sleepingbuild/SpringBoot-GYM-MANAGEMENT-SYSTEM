package com.gym.management.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Agent 6 - Hội viên có gói ACTIVE sắp hết hạn, để Sales liên hệ gia hạn.
 * Các trường rỗng bị bỏ khỏi JSON (phone, remainingSessions).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExpiringMemberResponse(
        UUID userId,
        String fullName,
        String phone,
        String email,
        String packageName,
        LocalDate endDate,
        /** Số ngày từ hôm nay đến ngày hết hạn, 0 nghĩa là hết hạn hôm nay. */
        long daysLeft,
        Integer remainingSessions) {
}

package com.gym.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Agent 6 - Tỷ lệ gia hạn. rate trong [0, 1], null nếu không có gói nào hết hạn trong kỳ. */
public record RenewalRateResponse(
        LocalDate from,
        LocalDate to,
        int windowDays,
        long expiredCount,
        long renewedCount,
        BigDecimal rate) {
}

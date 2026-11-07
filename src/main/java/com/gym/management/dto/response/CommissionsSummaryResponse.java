package com.gym.management.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent 6 - Tổng lương theo buổi của PT, gộp từ CommissionService#summarize(YearMonth) của Agent 5.
 * fromMonth và toMonth có dạng yyyy-MM, gồm cả hai đầu. months luôn đủ mọi tháng trong khoảng.
 */
public record CommissionsSummaryResponse(
        String fromMonth,
        String toMonth,
        int totalSessions,
        BigDecimal totalAmount,
        int pendingCount,
        BigDecimal pendingAmount,
        int paidCount,
        BigDecimal paidAmount,
        List<MonthItem> months) {

    /** month: yyyy-MM. */
    public record MonthItem(
            String month,
            int totalSessions,
            BigDecimal totalAmount,
            int pendingCount,
            BigDecimal pendingAmount,
            int paidCount,
            BigDecimal paidAmount) {
    }
}

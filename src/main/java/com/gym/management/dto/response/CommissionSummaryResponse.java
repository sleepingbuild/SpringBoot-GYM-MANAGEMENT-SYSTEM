package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Agent 5 - POS & Commission Module.
 * Dùng cho GET /api/v1/reports/commissions-summary (Agent 6) — Agent 6 gọi
 * {@code CommissionService#summarize(YearMonth)}, KHÔNG tự query bảng commissions.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSummaryResponse {
    private String month; // "yyyy-MM"
    private int totalSessions;
    private BigDecimal totalAmount;
    private int pendingCount;
    private BigDecimal pendingAmount;
    private int paidCount;
    private BigDecimal paidAmount;
}

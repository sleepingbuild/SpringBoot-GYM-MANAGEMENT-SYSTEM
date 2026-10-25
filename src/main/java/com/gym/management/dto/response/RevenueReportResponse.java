package com.gym.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Agent 6 - Báo cáo doanh thu (giao dịch SUCCESS, tách MEMBERSHIP và POS). */
public record RevenueReportResponse(
        LocalDate from,
        LocalDate to,
        String groupBy,
        BigDecimal total,
        BigDecimal membershipTotal,
        BigDecimal posTotal,
        List<Item> items) {

    /** period: yyyy-MM-dd (DAY) hoặc yyyy-MM (MONTH). */
    public record Item(String period, BigDecimal membership, BigDecimal pos, BigDecimal total) {
    }
}

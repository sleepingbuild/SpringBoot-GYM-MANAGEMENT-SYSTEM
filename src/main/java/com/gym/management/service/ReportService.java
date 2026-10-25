package com.gym.management.service;

import com.gym.management.dto.response.CheckinTrafficResponse;
import com.gym.management.dto.response.RenewalRateResponse;
import com.gym.management.dto.response.RevenueReportResponse;
import com.gym.management.dto.response.TopPackagesResponse;

/**
 * Agent 6 - Báo cáo (chỉ đọc). Tham số nhận dạng chuỗi và được kiểm tra trong service để
 * mọi giá trị sai trả VALIDATION_ERROR (400), không phụ thuộc handler kiểu tham số.
 */
public interface ReportService {

    /** groupBy: DAY (mặc định) hoặc MONTH. */
    RevenueReportResponse revenue(String from, String to, String groupBy);

    /** windowDays: 1 đến 365, mặc định 30. */
    RenewalRateResponse renewalRate(String from, String to, String windowDays);

    CheckinTrafficResponse checkinTraffic(String from, String to);

    /** limit: 1 đến 50, mặc định 10. */
    TopPackagesResponse topPackages(String from, String to, String limit);
}

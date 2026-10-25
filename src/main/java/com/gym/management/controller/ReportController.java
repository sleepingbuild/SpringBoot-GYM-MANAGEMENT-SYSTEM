package com.gym.management.controller;

import com.gym.management.dto.response.CheckinTrafficResponse;
import com.gym.management.dto.response.RenewalRateResponse;
import com.gym.management.dto.response.RevenueReportResponse;
import com.gym.management.dto.response.TopPackagesResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 6 - Báo cáo cho Super Admin. Truy vấn chỉ đọc. Khoảng ngày [from, to] gồm cả hai đầu,
 * tối đa 366 ngày, mặc định từ ngày 1 của tháng hiện tại đến hôm nay (giờ Asia/Ho_Chi_Minh).
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Reports", description = "Báo cáo cho Super Admin (chỉ đọc)")
public class ReportController {

    private static final String FROM_DESC = "Ngày bắt đầu (yyyy-MM-dd), mặc định ngày 1 của tháng hiện tại";
    private static final String TO_DESC = "Ngày kết thúc (yyyy-MM-dd), gồm cả ngày này, mặc định hôm nay";

    private final ReportService reportService;

    @GetMapping("/revenue")
    @Operation(summary = "Doanh thu theo ngày hoặc tháng",
            description = "Tổng giao dịch thanh toán SUCCESS, tách MEMBERSHIP và POS. Ngày ghi nhận là thời điểm "
                    + "giao dịch chuyển sang thành công. Tính theo tiền đã thu: đơn POS đã huỷ không bị trừ vì hệ thống "
                    + "không ghi nhận hoàn tiền. Các kỳ không có dữ liệu trả 0.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Báo cáo doanh thu")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ Super Admin")
    public ApiResponse<RevenueReportResponse> revenue(
            @Parameter(description = FROM_DESC) @RequestParam(required = false) String from,
            @Parameter(description = TO_DESC) @RequestParam(required = false) String to,
            @Parameter(description = "DAY (mặc định) hoặc MONTH") @RequestParam(required = false) String groupBy) {
        return ApiResponse.success(reportService.revenue(from, to, groupBy));
    }

    @GetMapping("/renewal-rate")
    @Operation(summary = "Tỷ lệ gia hạn gói",
            description = "Mẫu số: số gói có ngày hết hạn (end_date) trong kỳ và đã qua. Tử số: gói mà cùng hội viên "
                    + "có gói mới (ACTIVE, EXPIRED hoặc SCHEDULED) bắt đầu sau gói cũ và không muộn quá windowDays ngày "
                    + "sau ngày hết hạn. Đếm theo gói. Gói không có ngày hết hạn, gói PENDING, CANCELLED, SCHEDULED "
                    + "không tính. rate nằm trong [0, 1], null nếu không có gói hết hạn trong kỳ.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tỷ lệ gia hạn")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ Super Admin")
    public ApiResponse<RenewalRateResponse> renewalRate(
            @Parameter(description = FROM_DESC) @RequestParam(required = false) String from,
            @Parameter(description = TO_DESC) @RequestParam(required = false) String to,
            @Parameter(description = "Số ngày sau khi hết hạn vẫn tính là gia hạn, 1 đến 365, mặc định 30")
            @RequestParam(required = false) String windowDays) {
        return ApiResponse.success(reportService.renewalRate(from, to, windowDays));
    }

    @GetMapping("/checkin-traffic")
    @Operation(summary = "Lưu lượng lượt vào buổi PT theo ngày và giờ",
            description = "Đếm lượt học viên quét mặt vào buổi tập có PT (bookings.check_in_time). Hiện hệ thống chỉ "
                    + "ghi nhận check-in ở buổi PT, không có dữ liệu lượt vào phòng tập chung. Mỗi booking tính một lượt "
                    + "(học viên có hai booking trong ngày được đếm hai lần), kể cả booking đã huỷ sau khi đã quét mặt; "
                    + "lượt quét của PT không tính. Ngày và giờ là thời điểm quét, không phải giờ bắt đầu của buổi. "
                    + "Trả đủ 24 khung giờ.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lưu lượng")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ Super Admin")
    public ApiResponse<CheckinTrafficResponse> checkinTraffic(
            @Parameter(description = FROM_DESC) @RequestParam(required = false) String from,
            @Parameter(description = TO_DESC) @RequestParam(required = false) String to) {
        return ApiResponse.success(reportService.checkinTraffic(from, to));
    }

    @GetMapping("/top-packages")
    @Operation(summary = "Gói bán chạy",
            description = "Xếp theo số lượt đăng ký đã thanh toán thành công trong kỳ (mỗi giao dịch MEMBERSHIP SUCCESS "
                    + "là một lượt), kèm doanh thu của gói.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Danh sách gói bán chạy")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tham số không hợp lệ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Chỉ Super Admin")
    public ApiResponse<TopPackagesResponse> topPackages(
            @Parameter(description = FROM_DESC) @RequestParam(required = false) String from,
            @Parameter(description = TO_DESC) @RequestParam(required = false) String to,
            @Parameter(description = "Số gói tối đa, 1 đến 50, mặc định 10") @RequestParam(required = false) String limit) {
        return ApiResponse.success(reportService.topPackages(from, to, limit));
    }
}

package com.gym.management.controller;

import com.gym.management.dto.response.CommissionResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.CommissionService;
import com.gym.management.util.PageableUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Agent 5 - POS & Commission Module.
 * Chỉ còn lương theo buổi của PT — hoa hồng Sales đã bỏ (xem
 * docs/CHANGE_PT_ATTENDANCE_AND_PAY.md mục 1.6).
 */
@RestController
@RequestMapping("/api/v1/commissions")
@RequiredArgsConstructor
@Tag(name = "Commissions", description = "Lương PT theo buổi (Agent 5)")
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "Lịch sử lương theo buổi của chính PT đang đăng nhập, phân trang")
    public ApiResponse<PageResponse<CommissionResponse>> myCommissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(
                commissionService.myCommissions(CurrentUserId.get(), PageableUtils.of(page, size, sort)));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Tra cứu lương theo PT và/hoặc tháng (yyyy-MM), phân trang")
    public ApiResponse<PageResponse<CommissionResponse>> search(
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(
                commissionService.search(staffId, parseMonth(month), PageableUtils.of(page, size, sort)));
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Đánh dấu đã chi trả 1 bản ghi lương (PENDING → PAID)")
    public ApiResponse<CommissionResponse> markPaid(@PathVariable UUID id) {
        return ApiResponse.success("Đã đánh dấu thanh toán", commissionService.markPaid(id));
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "month không hợp lệ, định dạng đúng là yyyy-MM");
        }
    }
}

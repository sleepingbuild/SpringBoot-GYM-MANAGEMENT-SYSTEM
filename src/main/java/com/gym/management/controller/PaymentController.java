package com.gym.management.controller;

import com.gym.management.dto.request.PaymentConfirmRequest;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.PaymentService;
import com.gym.management.util.PageableUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * confirmPayment ở đây gọi PaymentService (interface DÙNG CHUNG — xem Javadoc PaymentService)
 * — Agent 5 (POS, từ M4) sẽ tái sử dụng đúng service này, không viết controller/service riêng.
 *
 * GET /payments?branchId=... trong API_DESIGN.md gốc đã HOÃN theo quyết định Phi
 * (payment_transactions không có branch_id) — thay bằng GET /payments?from=&to= theo khoảng ngày.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Xác nhận thanh toán tại chỗ — dùng chung Membership & POS (Agent 2)")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{transactionId}/confirm")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Lễ tân/Admin xác nhận đã thu tiền tại chỗ — chuyển giao dịch PENDING → SUCCESS")
    public ApiResponse<PaymentTransactionResponse> confirm(@PathVariable UUID transactionId,
                                                            @Valid @RequestBody(required = false) PaymentConfirmRequest request) {
        String method = request != null ? request.getPaymentMethod() : null;
        return ApiResponse.success("Xác nhận thanh toán thành công", paymentService.confirmPayment(transactionId, method));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER','TRAINER','RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Lịch sử thanh toán của tôi, phân trang")
    public ApiResponse<PageResponse<PaymentTransactionResponse>> myPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(paymentService.getMyPayments(CurrentUserId.get(), PageableUtils.of(page, size, sort)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Toàn bộ giao dịch theo khoảng ngày [from, to] — mặc định hôm nay nếu không truyền (Lễ tân/Admin)")
    public ApiResponse<PageResponse<PaymentTransactionResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(paymentService.getAllPayments(from, to, PageableUtils.of(page, size, sort)));
    }
}

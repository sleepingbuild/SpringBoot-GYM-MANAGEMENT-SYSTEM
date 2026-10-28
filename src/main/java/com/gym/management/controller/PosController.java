package com.gym.management.controller;

import com.gym.management.dto.request.PosOrderCreateRequest;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.PosOrderResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.PosOrderService;
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

@RestController
@RequestMapping("/api/v1/pos/orders")
@RequiredArgsConstructor
@Tag(name = "POS", description = "Bán hàng tại quầy (Agent 5)")
public class PosController {

    private final PosOrderService posOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Tạo đơn bán hàng tại quầy — thu tiền và trừ kho ngay trong request này")
    public ApiResponse<PosOrderResponse> create(@Valid @RequestBody PosOrderCreateRequest request) {
        return ApiResponse.success("Tạo đơn hàng thành công", posOrderService.createOrder(CurrentUserId.get(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Chi tiết 1 đơn hàng")
    public ApiResponse<PosOrderResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(posOrderService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Danh sách đơn hàng theo chi nhánh và khoảng ngày (mặc định hôm nay nếu không truyền), phân trang")
    public ApiResponse<PageResponse<PosOrderResponse>> search(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(posOrderService.search(branchId, from, to, PageableUtils.of(page, size, sort)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Huỷ đơn đã hoàn thành — hoàn lại tồn kho (không hoàn tiền, ngoài phạm vi schema hiện tại)")
    public ApiResponse<PosOrderResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Huỷ đơn hàng thành công", posOrderService.cancelOrder(id));
    }
}

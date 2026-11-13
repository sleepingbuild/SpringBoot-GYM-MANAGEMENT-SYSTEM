package com.gym.management.controller;

import com.gym.management.dto.request.MemberPosOrderRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Agent 5 - Hội viên tự mua hàng (POS cho member). Chỉ MEMBER, chỉ thấy và tạo đơn của chính mình. */
@RestController
@RequestMapping("/api/v1/pos/my-orders")
@RequiredArgsConstructor
@Tag(name = "POS", description = "Bán hàng tại quầy (Agent 5)")
public class MemberPosController {

    private final PosOrderService posOrderService;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Hội viên tự mua hàng — thanh toán mô phỏng thành công ngay, trừ kho ngay trong request này")
    public ApiResponse<PosOrderResponse> create(@Valid @RequestBody MemberPosOrderRequest request) {
        return ApiResponse.success("Mua hàng thành công", posOrderService.createOwnOrder(CurrentUserId.get(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Danh sách đơn hàng của chính hội viên, phân trang (mới nhất trước)")
    public ApiResponse<PageResponse<PosOrderResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(posOrderService.searchOwn(CurrentUserId.get(), PageableUtils.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Chi tiết 1 đơn hàng của chính hội viên (đơn của người khác trả 404)")
    public ApiResponse<PosOrderResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(posOrderService.getOwnById(CurrentUserId.get(), id));
    }
}

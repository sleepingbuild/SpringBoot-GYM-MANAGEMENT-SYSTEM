package com.gym.management.controller;

import com.gym.management.dto.request.SelfPurchaseRequest;
import com.gym.management.dto.response.PackagePurchaseResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.MemberSelfPurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member-packages")
@RequiredArgsConstructor
@Tag(name = "Membership")
public class MemberSelfPurchaseController {

    private final MemberSelfPurchaseService memberSelfPurchaseService;

    @PostMapping("/purchase")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Hội viên tự mua gói tập cho chính mình — thanh toán mô phỏng thành công ngay, gói ACTIVE luôn")
    public ApiResponse<PackagePurchaseResponse> purchase(@Valid @RequestBody SelfPurchaseRequest request) {
        return ApiResponse.success("Mua gói tập thành công, gói đã được kích hoạt",
                memberSelfPurchaseService.purchase(CurrentUserId.get(), request.getPackageId(), request.getBranchId()));
    }
}

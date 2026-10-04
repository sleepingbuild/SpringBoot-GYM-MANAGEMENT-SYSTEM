package com.gym.management.controller;

import com.gym.management.dto.request.RegisterPackageRequest;
import com.gym.management.dto.request.UpgradeDowngradeRequest;
import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.dto.response.PackagePurchaseResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.MembershipService;
import com.gym.management.util.PageableUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/member-packages")
@RequiredArgsConstructor
@Tag(name = "Membership", description = "Đăng ký / nâng cấp / hạ cấp gói tập (Agent 2)")
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Đăng ký gói tập MỚI — Member tự mua hoặc Lễ tân bán hộ (không phải nâng/hạ cấp)")
    public ApiResponse<PackagePurchaseResponse> register(@Valid @RequestBody RegisterPackageRequest request,
                                                         Authentication authentication) {
        UUID targetUserId = resolveTargetUserId(request.getUserId(), authentication);
        return ApiResponse.success("Đăng ký gói tập thành công, chờ xác nhận thanh toán",
                membershipService.registerNewPackage(targetUserId, request.getPackageId(), request.getBranchId()));
    }

    @PostMapping("/{id}/upgrade")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Nâng cấp gói tập — {id} là member_package đang ACTIVE muốn đổi")
    public ApiResponse<PackagePurchaseResponse> upgrade(@PathVariable UUID id,
                                                        @Valid @RequestBody UpgradeDowngradeRequest request,
                                                        Authentication authentication) {
        boolean isStaff = isStaffRole(authentication, "RECEPTIONIST", "SUPER_ADMIN");
        return ApiResponse.success("Nâng cấp gói tập thành công, chờ xác nhận thanh toán",
                membershipService.upgradePackage(id, request.getNewPackageId(), request.getBranchId(),
                        CurrentUserId.get(), isStaff));
    }

    @PostMapping("/{id}/downgrade")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Hạ cấp gói tập — {id} là member_package đang ACTIVE muốn đổi")
    public ApiResponse<PackagePurchaseResponse> downgrade(@PathVariable UUID id,
                                                          @Valid @RequestBody UpgradeDowngradeRequest request,
                                                          Authentication authentication) {
        boolean isStaff = isStaffRole(authentication, "RECEPTIONIST", "SUPER_ADMIN");
        return ApiResponse.success("Đã lên lịch hạ cấp gói tập",
                membershipService.downgradePackage(id, request.getNewPackageId(), request.getBranchId(),
                        CurrentUserId.get(), isStaff));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER','TRAINER','RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Gói tập hiện tại của tôi (dùng getCurrentMembership — nguồn chân lý duy nhất)")
    public ApiResponse<MemberPackageResponse> getMyCurrentMembership() {
        return ApiResponse.success(membershipService.getCurrentMembershipResponse(CurrentUserId.get()));
    }

    @GetMapping("/me/history")
    @PreAuthorize("hasAnyRole('MEMBER','TRAINER','RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Lịch sử toàn bộ gói tập đã đăng ký của tôi, phân trang")
    public ApiResponse<PageResponse<MemberPackageResponse>> getMyHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(membershipService.getHistory(CurrentUserId.get(), PageableUtils.of(page, size, sort)));
    }

    private UUID resolveTargetUserId(UUID requestedUserId, Authentication authentication) {
        UUID callerId = CurrentUserId.get();
        if (requestedUserId == null || requestedUserId.equals(callerId)) {
            return callerId;
        }
        if (!isStaffRole(authentication, "RECEPTIONIST", "SUPER_ADMIN")) {
            throw new AccessDeniedException("Chỉ Lễ tân/Admin mới được đăng ký gói hộ người khác");
        }
        return requestedUserId;
    }


    private boolean isStaffRole(Authentication authentication, String... roles) {
        if (authentication == null) {
            return false;
        }
        for (String role : roles) {
            String authority = "ROLE_" + role;
            for (var grantedAuthority : authentication.getAuthorities()) {
                if (grantedAuthority.getAuthority().equals(authority)) {
                    return true;
                }
            }
        }
        return false;
    }
}
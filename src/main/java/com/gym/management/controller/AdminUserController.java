package com.gym.management.controller;

import com.gym.management.dto.request.AdminUserCreateRequest;
import com.gym.management.dto.request.AdminUserRolesRequest;
import com.gym.management.dto.request.AdminUserStatusRequest;
import com.gym.management.dto.request.AdminUserUpdateRequest;
import com.gym.management.dto.response.AdminUserResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUser;
import com.gym.management.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Agent 1. Quản trị tài khoản: chỉ Super Admin. */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Tìm hội viên và quản trị tài khoản (Agent 1)")
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Danh sách khách hàng hoặc nhân viên, tìm theo tên/email/số điện thoại (Super Admin)")
    public ApiResponse<PageResponse<AdminUserResponse>> search(
            @RequestParam(defaultValue = "CUSTOMER") String group,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userAdminService.search(group, q, status, page, size));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Tạo tài khoản nhân viên với vai trò chọn trước (Super Admin)")
    public ApiResponse<AdminUserResponse> createStaff(@Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success("Tạo nhân viên thành công", userAdminService.createStaff(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sửa họ tên và số điện thoại của tài khoản (Super Admin)")
    public ApiResponse<AdminUserResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.success("Cập nhật tài khoản thành công", userAdminService.update(id, request));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Gán vai trò cho tài khoản; không cho tự đổi vai trò của mình (Super Admin)")
    public ApiResponse<AdminUserResponse> setRoles(@CurrentUser UUID currentUserId,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody AdminUserRolesRequest request) {
        return ApiResponse.success("Đã cập nhật vai trò",
                userAdminService.setRoles(currentUserId, id, request.getRoles()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Khoá hoặc mở tài khoản; không cho tự khoá mình (Super Admin)")
    public ApiResponse<AdminUserResponse> setStatus(@CurrentUser UUID currentUserId,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody AdminUserStatusRequest request) {
        return ApiResponse.success("Đã cập nhật trạng thái",
                userAdminService.setStatus(currentUserId, id, request.getStatus()));
    }
}

package com.gym.management.controller;

import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.UserSummaryResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.DirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Agent 1. Tìm hội viên cho lễ tân và quản trị viên (chọn hội viên khi bán gói hộ, đặt lịch hộ). */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Tìm hội viên (Agent 1)")
public class UserController {

    private final DirectoryService directoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Tìm hội viên đang hoạt động theo tên, số điện thoại hoặc email (Lễ tân, Super Admin)")
    public ApiResponse<PageResponse<UserSummaryResponse>> searchMembers(
            @RequestParam(defaultValue = "MEMBER") String role,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(directoryService.searchMembers(role, q, page, size));
    }
}

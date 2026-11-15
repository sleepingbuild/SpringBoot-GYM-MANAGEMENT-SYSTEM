package com.gym.management.controller;

import com.gym.management.dto.response.MemberStatsResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.MemberStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Thống kê hội viên", description = "Thống kê cá nhân của hội viên trong 30 ngày gần nhất (chỉ đọc)")
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class MemberStatsController {

    private final MemberStatsService memberStatsService;

    @Operation(summary = "Thống kê cá nhân: số buổi còn lại, số buổi đã tập, số lần vào phòng tập, số booking theo trạng thái (30 ngày gần nhất)")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('MEMBER')")
    public ApiResponse<MemberStatsResponse> myStats() {
        return ApiResponse.success(memberStatsService.getMyStats(CurrentUserId.get()));
    }
}

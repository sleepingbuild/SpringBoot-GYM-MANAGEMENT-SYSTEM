package com.gym.management.controller;

import com.gym.management.exception.ApiResponse;
import com.gym.management.security.CurrentUserId;
import com.gym.management.service.CheckInHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Check-in", description = "Lịch sử vào phòng tập của hội viên (lượt tự do và buổi có PT)")
@RestController
@RequestMapping("/api/v1/check-ins")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInHistoryService checkInHistoryService;

    @Operation(summary = "Xem lịch sử check-in của chính mình (lượt vào phòng không PT và buổi có PT), mới nhất trước")
    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public ApiResponse<?> myCheckIns() {
        return ApiResponse.success(checkInHistoryService.findMyHistory(CurrentUserId.get()));
    }
}

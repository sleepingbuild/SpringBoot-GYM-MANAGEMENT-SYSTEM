package com.gym.management.controller;

import com.gym.management.dto.request.TrainerPayRateRequest;
import com.gym.management.dto.response.TrainerPayRateResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.TrainerPayRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trainer-pay-rates")
@RequiredArgsConstructor
@Tag(name = "TrainerPayRates", description = "Mức lương mỗi buổi của PT (Agent 5)")
public class TrainerPayRateController {

    private final TrainerPayRateService trainerPayRateService;

    @GetMapping("/{trainerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Xem mức lương mỗi buổi hiện tại của 1 PT")
    public ApiResponse<TrainerPayRateResponse> getByTrainerId(@PathVariable UUID trainerId) {
        return ApiResponse.success(trainerPayRateService.getByTrainerId(trainerId));
    }

    @PutMapping("/{trainerId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Đặt/cập nhật mức lương mỗi buổi của 1 PT")
    public ApiResponse<TrainerPayRateResponse> upsert(@PathVariable UUID trainerId,
                                                        @Valid @RequestBody TrainerPayRateRequest request) {
        return ApiResponse.success("Cập nhật mức lương thành công", trainerPayRateService.upsert(trainerId, request));
    }
}

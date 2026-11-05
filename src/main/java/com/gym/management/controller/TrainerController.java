package com.gym.management.controller;

import com.gym.management.dto.response.TrainerResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.DirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Agent 1. Danh sách PT cho màn hình đặt lịch. Mọi người đã đăng nhập đều xem được; chỉ trả thông tin công khai. */
@RestController
@RequestMapping("/api/v1/trainers")
@RequiredArgsConstructor
@Tag(name = "Trainers", description = "Danh sách huấn luyện viên (Agent 1)")
public class TrainerController {

    private final DirectoryService directoryService;

    @GetMapping
    @Operation(summary = "Danh sách huấn luyện viên đang hoạt động (id, tên, ảnh đại diện)")
    public ApiResponse<List<TrainerResponse>> list() {
        return ApiResponse.success(directoryService.listTrainers());
    }
}

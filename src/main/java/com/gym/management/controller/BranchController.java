package com.gym.management.controller;

import com.gym.management.dto.response.BranchResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.DirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Agent 1. Danh sách chi nhánh cho các màn hình cần chọn chi nhánh. Mọi người đã đăng nhập đều xem được. */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Danh sách chi nhánh (Agent 1)")
public class BranchController {

    private final DirectoryService directoryService;

    @GetMapping
    @Operation(summary = "Danh sách chi nhánh đang hoạt động (id, tên)")
    public ApiResponse<List<BranchResponse>> list() {
        return ApiResponse.success(directoryService.listBranches());
    }
}

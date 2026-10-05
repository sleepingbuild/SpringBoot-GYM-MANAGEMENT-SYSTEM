package com.gym.management.controller;

import com.gym.management.dto.request.PackageRequest;
import com.gym.management.dto.response.PackageResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.PackageService;
import com.gym.management.util.PageableUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Agent 2 - Membership, Package & Payment Module.
 *
 * Đã chốt với Phi (xem Agent2-Respone.md mục 3.1): tách GET (mọi role đã đăng nhập) khỏi
 * POST/PUT/DELETE (Super Admin) — khác với dòng gộp chung trong API_DESIGN.md bản gốc;
 * API_DESIGN.md cần cập nhật lại cho khớp (xem AGENT2_README.md).
 */
@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
@Tag(name = "Packages", description = "Quản lý gói tập (Agent 2)")
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    @Operation(summary = "Danh sách gói tập đang mở bán, phân trang (public — mọi role đã đăng nhập)")
    public ApiResponse<PageResponse<PackageResponse>> listActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(packageService.listActive(PageableUtils.of(page, size, sort)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Danh sách toàn bộ gói tập kể cả đã ngừng bán, phân trang (Super Admin)")
    public ApiResponse<PageResponse<PackageResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(packageService.listAll(PageableUtils.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết 1 gói tập")
    public ApiResponse<PackageResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(packageService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Tạo gói tập mới (Super Admin)")
    public ApiResponse<PackageResponse> create(@Valid @RequestBody PackageRequest request) {
        return ApiResponse.success("Tạo gói tập thành công", packageService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Cập nhật gói tập (Super Admin)")
    public ApiResponse<PackageResponse> update(@PathVariable UUID id, @Valid @RequestBody PackageRequest request) {
        return ApiResponse.success("Cập nhật gói tập thành công", packageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Ngừng bán gói tập — soft delete (is_active = false), Super Admin")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        packageService.deactivate(id);
        return ApiResponse.success("Đã ngừng bán gói tập", null);
    }
}

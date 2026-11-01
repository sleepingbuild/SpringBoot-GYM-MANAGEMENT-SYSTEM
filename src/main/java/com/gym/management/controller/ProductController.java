package com.gym.management.controller;

import com.gym.management.dto.request.ProductRequest;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.ProductResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.ProductService;
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
 * Agent 5 - POS & Commission Module.
 * GET mở cho mọi role đã đăng nhập (member xem được giá để biết trước khi ra quầy);
 * ghi (POST/PUT/DELETE) chỉ Lễ tân/Admin — cùng cách PackageController (Agent 2) đã tách.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Sản phẩm bán tại quầy POS (Agent 5)")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Danh sách sản phẩm đang bán, phân trang")
    public ApiResponse<PageResponse<ProductResponse>> listActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(productService.listActive(PageableUtils.of(page, size, sort)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Danh sách toàn bộ sản phẩm kể cả đã ngừng bán, phân trang")
    public ApiResponse<PageResponse<ProductResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ApiResponse.success(productService.listAll(PageableUtils.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết 1 sản phẩm")
    public ApiResponse<ProductResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(productService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Tạo sản phẩm mới")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Tạo sản phẩm thành công", productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Cập nhật sản phẩm")
    public ApiResponse<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success("Cập nhật sản phẩm thành công", productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','SUPER_ADMIN')")
    @Operation(summary = "Ngừng bán sản phẩm — soft delete (is_active = false)")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        productService.deactivate(id);
        return ApiResponse.success("Đã ngừng bán sản phẩm", null);
    }
}

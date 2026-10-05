package com.gym.management.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * Parse tham số phân trang theo đúng chuẩn API_DESIGN.md: "?page=0&size=20&sort=createdAt,desc".
 * Đặt ở package util (không phải SHARED) — nếu Agent 1/agent khác đã có sẵn helper tương tự,
 * dùng bản đó thay và xoá file này để tránh trùng lặp.
 */
public final class PageableUtils {

    private PageableUtils() {
    }

    public static Pageable of(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // chặn size quá lớn gây tải nặng DB
        return PageRequest.of(safePage, safeSize, parseSort(sort));
    }

    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}

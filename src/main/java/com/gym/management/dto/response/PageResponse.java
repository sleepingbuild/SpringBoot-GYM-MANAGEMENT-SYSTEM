package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Agent 2 - Membership, Package & Payment Module.
 * Wrapper chuẩn cho danh sách phân trang — khớp API_DESIGN.md mục 2
 * ("?page=0&size=20&sort=createdAt,desc" → PageResponse<T>").
 *
 * Không đặt trong package dùng chung của Agent 1 vì file đó không nằm trong danh sách
 * SHARED được liệt kê ở TASK_ASSIGNMENT.md — nếu Agent 1 hoặc agent khác đã có sẵn 1 bản
 * tương đương, xoá bản này và trỏ lại import cho khớp (tránh 2 class cùng vai trò).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <S, T> PageResponse<T> from(Page<S> source, Function<S, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages()
        );
    }
}

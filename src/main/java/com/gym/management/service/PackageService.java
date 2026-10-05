package com.gym.management.service;

import com.gym.management.dto.request.PackageRequest;
import com.gym.management.dto.response.PackageResponse;
import com.gym.management.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PackageService {

    PackageResponse create(PackageRequest request);

    PackageResponse update(UUID id, PackageRequest request);

    /** Soft-delete: set is_active = false, KHÔNG xoá cứng (nhiều member_packages có thể tham chiếu). */
    void deactivate(UUID id);

    PackageResponse getById(UUID id);

    /** @deprecated dùng {@link #listActive(Pageable)} — giữ lại cho nơi cần List thuần (nội bộ). */
    @Deprecated
    List<PackageResponse> listActive();

    /** Danh sách công khai có phân trang (Member xem để mua) — chỉ gói đang is_active = true. */
    PageResponse<PackageResponse> listActive(Pageable pageable);

    /** Danh sách đầy đủ kể cả đã ngừng bán, có phân trang — chỉ Super Admin. */
    PageResponse<PackageResponse> listAll(Pageable pageable);
}

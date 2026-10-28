package com.gym.management.service;

import com.gym.management.dto.request.ProductRequest;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Agent 5 - POS & Commission Module.
 */
public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse update(UUID id, ProductRequest request);

    void deactivate(UUID id);

    ProductResponse getById(UUID id);

    PageResponse<ProductResponse> listActive(Pageable pageable);

    PageResponse<ProductResponse> listAll(Pageable pageable);
}

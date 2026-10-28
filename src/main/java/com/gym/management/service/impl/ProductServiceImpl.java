package com.gym.management.service.impl;

import com.gym.management.dto.request.ProductRequest;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.dto.response.ProductResponse;
import com.gym.management.entity.Product;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.ProductRepository;
import com.gym.management.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request, true);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        applyRequest(product, request, false);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public ProductResponse getById(UUID id) {
        return toResponse(productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)));
    }

    @Override
    public PageResponse<ProductResponse> listActive(Pageable pageable) {
        return PageResponse.from(productRepository.findByActiveTrue(pageable), this::toResponse);
    }

    @Override
    public PageResponse<ProductResponse> listAll(Pageable pageable) {
        return PageResponse.from(productRepository.findAll(pageable), this::toResponse);
    }

    private void applyRequest(Product product, ProductRequest request, boolean isCreate) {
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setUnit(request.getUnit());
        if (isCreate) {
            product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        } else if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .unit(product.getUnit())
                .active(product.isActive())
                .build();
    }
}

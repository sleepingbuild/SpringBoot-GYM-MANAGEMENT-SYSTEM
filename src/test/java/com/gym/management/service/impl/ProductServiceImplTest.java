package com.gym.management.service.impl;

import com.gym.management.dto.request.ProductRequest;
import com.gym.management.dto.response.ProductResponse;
import com.gym.management.entity.Product;
import com.gym.management.entity.ProductCategory;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductServiceImpl service;

    private ProductRequest request() {
        ProductRequest r = new ProductRequest();
        r.setName("Whey 2lb");
        r.setCategory(ProductCategory.SUPPLEMENT);
        r.setPrice(new BigDecimal("500000"));
        return r;
    }

    @Test
    void create_defaultsStockToZeroAndActiveTrue() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse res = service.create(request());

        assertThat(res.getStockQuantity()).isZero();
        assertThat(res.isActive()).isTrue();
    }

    @Test
    void update_withNullStock_keepsExistingStock() {
        Product p = new Product();
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(p, "id", id);
        p.setStockQuantity(12);
        when(productRepository.findById(id)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse res = service.update(id, request());

        assertThat(res.getStockQuantity()).isEqualTo(12);
    }

    @Test
    void deactivate_isSoftDelete() {
        Product p = new Product();
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.of(p));

        service.deactivate(id);

        assertThat(p.isActive()).isFalse();
    }

    @Test
    void getById_unknown_throwsProductNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}

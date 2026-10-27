package com.gym.management.dto.response;

import com.gym.management.entity.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private ProductCategory category;
    private BigDecimal price;
    private int stockQuantity;
    private String unit;
    private boolean active;
}

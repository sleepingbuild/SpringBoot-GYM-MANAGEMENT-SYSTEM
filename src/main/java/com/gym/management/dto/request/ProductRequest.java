package com.gym.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotNull(message = "Danh mục sản phẩm không được để trống")
    private com.gym.management.entity.ProductCategory category;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price;

    private Integer stockQuantity; // null khi tạo mới = 0; null khi update = giữ nguyên

    private String unit;

    private Boolean active;
}

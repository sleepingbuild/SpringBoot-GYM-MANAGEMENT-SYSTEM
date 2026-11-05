package com.gym.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 150, message = "Tên sản phẩm tối đa 150 ký tự")
    private String name;

    @NotNull(message = "Danh mục sản phẩm không được để trống")
    private com.gym.management.entity.ProductCategory category;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá sản phẩm tối đa 10 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal price;

    @Min(value = 0, message = "Tồn kho không được âm")
    @Max(value = 1_000_000, message = "Tồn kho tối đa 1.000.000")
    private Integer stockQuantity; // null khi tạo mới = 0; null khi update = giữ nguyên

    @Size(max = 20, message = "Đơn vị tối đa 20 ký tự")
    private String unit;

    private Boolean active;
}

package com.gym.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Agent 5 - POS & Commission Module.
 * Sản phẩm bán tại quầy (nước uống, thực phẩm bổ sung, phụ kiện). Xoá mềm bằng
 * {@code is_active} — không bao giờ DELETE cứng vì pos_order_items còn tham chiếu lịch sử bán.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    @Column(length = 20)
    private String unit;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}

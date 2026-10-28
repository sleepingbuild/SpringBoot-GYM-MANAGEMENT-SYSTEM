package com.gym.management.dto.response;

import com.gym.management.entity.PosOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosOrderResponse {
    private UUID id;
    private UUID memberId;
    private UUID staffId;
    private UUID branchId;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private PosOrderStatus status;
    private UUID paymentTransactionId;
    private List<Item> items;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private UUID productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}

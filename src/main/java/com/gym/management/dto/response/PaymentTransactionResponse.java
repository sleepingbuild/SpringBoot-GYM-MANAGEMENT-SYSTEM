package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {
    private UUID id;
    private UUID userId;
    private String relatedType;
    private UUID relatedId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String transactionCode;
    private LocalDateTime createdAt;
}

package com.gym.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TrainerPayRateRequest {

    @NotNull(message = "Lương mỗi buổi không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Lương mỗi buổi phải lớn hơn 0")
    private BigDecimal amountPerSession;
}

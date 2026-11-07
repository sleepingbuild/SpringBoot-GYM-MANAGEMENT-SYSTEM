package com.gym.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TrainerPayRateRequest {

    @NotNull(message = "Lương mỗi buổi không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Lương mỗi buổi phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Lương mỗi buổi tối đa 10 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal amountPerSession;
}

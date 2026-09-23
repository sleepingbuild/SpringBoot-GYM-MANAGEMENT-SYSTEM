package com.gym.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateProfileRequest {

    @Min(value = 0, message = "Tuổi không hợp lệ")
    private Integer age;

    @DecimalMin(value = "0", inclusive = false, message = "Cân nặng không hợp lệ")
    private BigDecimal weightKg;

    @DecimalMin(value = "0", inclusive = false, message = "Chiều cao không hợp lệ")
    private BigDecimal heightCm;

    @Size(max = 255, message = "Mục tiêu tối đa 255 ký tự")
    private String goal;
}

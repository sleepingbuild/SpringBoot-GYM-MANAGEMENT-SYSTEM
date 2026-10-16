package com.gym.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeLeadStatusRequest {

    /** CONTACTED hoặc LOST. CONVERTED phải đi qua /convert. */
    @NotBlank(message = "Trạng thái không được để trống")
    private String status;
}

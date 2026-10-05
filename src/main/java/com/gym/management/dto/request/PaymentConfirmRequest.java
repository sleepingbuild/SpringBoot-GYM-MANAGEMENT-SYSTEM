package com.gym.management.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentConfirmRequest {

    /** Để trống = giữ nguyên method mặc định lúc tạo giao dịch (CASH). */
    @Pattern(regexp = "CASH|LOCAL_CONFIRM", message = "payment_method không hợp lệ")
    private String paymentMethod;
}

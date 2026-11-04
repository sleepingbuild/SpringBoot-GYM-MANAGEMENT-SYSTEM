package com.gym.management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PosOrderCreateRequest {

    /** null = khách vãng lai. */
    private UUID memberId;

    private UUID branchId;

    @NotNull(message = "Hình thức thanh toán không được để trống")
    @Pattern(regexp = "CASH|LOCAL_CONFIRM", message = "payment_method không hợp lệ")
    private String paymentMethod = "CASH";

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Size(max = 100, message = "Đơn hàng tối đa 100 dòng")
    @Valid
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {

        @NotNull(message = "productId không được để trống")
        private UUID productId;

        @Positive(message = "Số lượng phải lớn hơn 0")
        @Max(value = 1000, message = "Mỗi dòng tối đa 1000")
        private int quantity;
    }
}

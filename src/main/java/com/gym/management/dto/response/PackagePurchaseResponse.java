package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả trả về của mọi thao tác tạo/đổi member_package (đăng ký mới, nâng cấp, hạ cấp) —
 * gộp cả bản ghi member_package vừa tạo VÀ payment_transaction tương ứng, để FE không cần
 * gọi thêm request lấy transactionId dùng cho bước confirm thanh toán.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackagePurchaseResponse {
    private MemberPackageResponse memberPackage;
    private PaymentTransactionResponse paymentTransaction;
}

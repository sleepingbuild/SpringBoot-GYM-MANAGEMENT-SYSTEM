package com.gym.management.service;

import com.gym.management.dto.response.PackagePurchaseResponse;

import java.util.UUID;

/**
 * Agent 2 - Hội viên tự mua gói với thanh toán MÔ PHỎNG (không cổng thật): ghi nhận thành công ngay,
 * gói chuyển ACTIVE ngay. Vẫn đi qua {@link PaymentService#confirmPayment} nên sự kiện
 * PaymentConfirmedEvent và MembershipPaymentListener chạy đúng như luồng lễ tân xác nhận.
 *
 * Luồng lễ tân bán hộ (đăng ký -> PENDING -> xác nhận) KHÔNG đổi, vẫn dùng MembershipService.registerNewPackage.
 */
public interface MemberSelfPurchaseService {

    PackagePurchaseResponse purchase(UUID userId, UUID packageId, UUID branchId);
}

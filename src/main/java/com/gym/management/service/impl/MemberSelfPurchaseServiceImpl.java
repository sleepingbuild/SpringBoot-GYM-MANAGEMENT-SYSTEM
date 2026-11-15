package com.gym.management.service.impl;

import com.gym.management.constant.PaymentMethodType;
import com.gym.management.dto.response.MemberPackageResponse;
import com.gym.management.dto.response.PackagePurchaseResponse;
import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.service.MemberSelfPurchaseService;
import com.gym.management.service.MembershipService;
import com.gym.management.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Phụ thuộc 1 chiều: bean này -> MembershipService và PaymentService; không bean nào trong hai bên
 * phụ thuộc ngược lại, nên không tạo lại vòng phụ thuộc bean cũ.
 *
 * Toàn bộ chạy trong MỘT transaction: nếu bước thanh toán/kích hoạt lỗi thì gói PENDING vừa tạo cũng bị
 * rollback, không để lại gói treo.
 */
@Service
@RequiredArgsConstructor
public class MemberSelfPurchaseServiceImpl implements MemberSelfPurchaseService {

    private final MembershipService membershipService;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public PackagePurchaseResponse purchase(UUID userId, UUID packageId, UUID branchId) {
        // Dùng lại đúng luật đăng ký hiện có (gói phải đang mở bán, chặn khi đã có gói PENDING...).
        PackagePurchaseResponse registered = membershipService.registerNewPackage(userId, packageId, branchId);

        // Thanh toán mô phỏng: xác nhận thành công ngay. confirmPayment phát PaymentConfirmedEvent đồng bộ,
        // MembershipPaymentListener kích hoạt gói (PENDING -> ACTIVE, ghi đè ngày bắt đầu/kết thúc, số buổi).
        PaymentTransactionResponse confirmed = paymentService.confirmPayment(
                registered.getPaymentTransaction().getId(), PaymentMethodType.LOCAL_CONFIRM);

        // registered.getMemberPackage() là ảnh chụp lúc còn PENDING nên không dùng. Gói vừa kích hoạt là gói
        // ACTIVE mới nhất của người này (getCurrentMembership sắp theo createdAt giảm dần).
        MemberPackageResponse active = membershipService.getCurrentMembershipResponse(userId);

        return PackagePurchaseResponse.builder()
                .memberPackage(active)
                .paymentTransaction(confirmed)
                .build();
    }
}

package com.gym.management.service.listener;

import com.gym.management.constant.RelatedType;
import com.gym.management.service.MembershipService;
import com.gym.management.service.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Agent 2 - Membership, Package & Payment Module.
 *
 * Nối Payment → Membership theo 1 chiều qua sự kiện: khi 1 giao dịch MEMBERSHIP được xác nhận thành công,
 * kích hoạt member_package tương ứng. Đây là điểm nối DUY NHẤT giữa Payment và Membership — không lặp lại
 * logic activate ở nơi khác, và PaymentServiceImpl KHÔNG được inject MembershipService (sẽ tạo lại vòng
 * phụ thuộc bean làm ứng dụng không khởi động được).
 *
 * Chạy đồng bộ, chung transaction với {@code PaymentService#confirmPayment} (không dùng @Async,
 * không dùng @TransactionalEventListener) để việc xác nhận thanh toán và kích hoạt gói thành công/thất bại cùng nhau.
 */
@Component
@RequiredArgsConstructor
public class MembershipPaymentListener {

    private final MembershipService membershipService;

    @EventListener
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        if (RelatedType.MEMBERSHIP.equals(event.relatedType())) {
            membershipService.activateFromPendingPayment(event.relatedId());
        }
        // relatedType khác (POS...): không thuộc module Membership, bỏ qua.
    }
}

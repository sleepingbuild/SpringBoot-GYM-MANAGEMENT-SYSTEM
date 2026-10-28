package com.gym.management.entity;

/**
 * Agent 5 - POS & Commission Module.
 * PENDING chỉ tồn tại trong lúc tạo đơn (trước khi xác nhận thanh toán qua
 * {@link com.gym.management.service.PaymentService#confirmPayment}) — luồng mặc định
 * (lễ tân thu tiền tại quầy ngay lúc tạo đơn) chuyển thẳng PENDING → COMPLETED trong cùng
 * 1 transaction, nên thực tế hiếm khi quan sát được trạng thái PENDING "sống" ngoài DB.
 */
public enum PosOrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}

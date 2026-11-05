package com.gym.management.service;

import com.gym.management.dto.response.PaymentTransactionResponse;
import com.gym.management.entity.PaymentTransaction;
import com.gym.management.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Agent 2 - Membership, Package & Payment Module.
 *
 * ⚠️ 1 NGUỒN CHÂN LÝ cho việc xác nhận thanh toán (xem ISSUES.md 2.8) — dùng chung
 * cho CẢ Membership (module này) LẪN POS (Agent 5, từ M4). Agent 5 KHÔNG tự viết
 * service xác nhận thanh toán riêng, chỉ gọi lại {@link #createPendingTransaction}
 * (nếu POS cần bước chờ xác nhận) hoặc {@link #confirmPayment} (transactionId đã có).
 */
public interface PaymentService {

    /**
     * Tạo 1 bản ghi payment_transactions ở trạng thái PENDING — dùng khi luồng nghiệp vụ
     * cần tách rời bước "phát sinh giao dịch" và "xác nhận đã thu tiền" (đúng luồng Membership:
     * đăng ký → PENDING → lễ tân xác nhận → ACTIVE).
     */
    PaymentTransactionResponse createPendingTransaction(User user, String relatedType, UUID relatedId, BigDecimal amount);

    /**
     * Xác nhận 1 giao dịch đã tồn tại (PENDING → SUCCESS). Idempotent: gọi lại khi đã SUCCESS
     * thì trả nguyên trạng, không lỗi và KHÔNG phát lại sự kiện. Lần xác nhận đầu tiên phát
     * {@link com.gym.management.service.event.PaymentConfirmedEvent} (đồng bộ, chung transaction): với
     * relatedType = MEMBERSHIP, {@code MembershipPaymentListener} gọi
     * {@link MembershipService#activateFromPendingPayment} để kích hoạt gói tương ứng — đây là điểm nối DUY NHẤT
     * giữa Payment và Membership, không lặp lại logic activate ở nơi khác. Payment KHÔNG inject
     * MembershipService (tránh vòng phụ thuộc bean làm ứng dụng không khởi động được).
     */
    PaymentTransactionResponse confirmPayment(UUID transactionId, String paymentMethod);

    /**
     * Hoàn tiền 1 giao dịch POS khi đơn bị huỷ (SUCCESS → REFUNDED). Idempotent: đã REFUNDED thì trả nguyên
     * trạng, KHÔNG hoàn lần 2. Chỉ hoàn giao dịch POS đã SUCCESS: giao dịch gói tập (chưa hỗ trợ hoàn tiền)
     * hoặc giao dịch chưa SUCCESS → PAYMENT_NOT_REFUNDABLE. Báo cáo doanh thu chỉ cộng SUCCESS nên tự loại
     * giao dịch đã hoàn. Việc trả tồn kho khi huỷ đơn là của POS, không thuộc method này.
     */
    PaymentTransactionResponse refundPayment(UUID transactionId);

    /**
     * Hoàn tiền theo thực thể nghiệp vụ mà giao dịch gắn vào — để module khác (POS huỷ đơn) không phải lưu id giao dịch.
     * Tên và ngữ nghĩa theo quyết định của Agent 1. {@code relatedType} là chuỗi hằng của
     * {@code RelatedType} (lớp hằng, KHÔNG phải enum).
     * <ul>
     *   <li>{@code POS}: tìm giao dịch MỚI NHẤT của đơn (cột pos_order_id) rồi hoàn như {@link #refundPayment}.
     *       Gọi lần hai khi đã REFUNDED: không lỗi, không hoàn thêm (huỷ đơn có thể được thử lại).
     *       Đơn chưa có giao dịch nào, hoặc giao dịch chưa SUCCESS → PAYMENT_NOT_REFUNDABLE.</li>
     *   <li>{@code MEMBERSHIP}: gói tập chưa hỗ trợ hoàn tiền → PAYMENT_NOT_REFUNDABLE.</li>
     *   <li>{@code relatedType} khác hoặc {@code relatedId} null → VALIDATION_ERROR.</li>
     * </ul>
     */
    PaymentTransactionResponse refundByRelated(String relatedType, UUID relatedId);

    PaymentTransaction getEntityById(UUID transactionId);

    /** @deprecated dùng {@link #getMyPayments(UUID, org.springframework.data.domain.Pageable)}. */
    @Deprecated
    List<PaymentTransactionResponse> getMyPayments(UUID userId);

    com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getMyPayments(UUID userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Thay cho GET /payments?branchId=... (hoãn theo quyết định Phi — payment_transactions không có branch_id).
     * Lễ tân/Admin tra cứu giao dịch, có thể lọc thêm theo trạng thái và người mua (H3, frontend đợt 1).
     *
     * Quy tắc ngày: truyền {@code from} hoặc {@code to} thì lọc theo khoảng ngày (thiếu vế nào mặc định hôm nay).
     * Không truyền cả hai: nếu cũng không có {@code status}/{@code userId} thì lấy hôm nay (như trước);
     * nếu có {@code status} hoặc {@code userId} thì lấy MỌI ngày — để lễ tân tìm giao dịch chờ xác nhận cũ.
     * {@code status} không hợp lệ (ngoài PENDING, SUCCESS, FAILED, REFUNDED) → VALIDATION_ERROR.
     */
    com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getAllPayments(
            java.time.LocalDate from, java.time.LocalDate to, String status, UUID userId,
            org.springframework.data.domain.Pageable pageable);

    /** Bản không lọc trạng thái/người mua: giữ để code cũ gọi 3 tham số vẫn chạy. */
    default com.gym.management.dto.response.PageResponse<PaymentTransactionResponse> getAllPayments(
            java.time.LocalDate from, java.time.LocalDate to, org.springframework.data.domain.Pageable pageable) {
        return getAllPayments(from, to, null, null, pageable);
    }
}

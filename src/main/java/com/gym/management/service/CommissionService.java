package com.gym.management.service;

import com.gym.management.dto.response.CommissionResponse;
import com.gym.management.dto.response.CommissionSummaryResponse;
import com.gym.management.dto.response.PageResponse;
import com.gym.management.entity.CommissionStatus;
import com.gym.management.service.event.BookingSettledEvent;
import org.springframework.data.domain.Pageable;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Agent 5 - POS & Commission Module.
 *
 * ⚠️ 1 NGUỒN CHÂN LÝ cho việc ghi nhận lương PT theo buổi — Agent 6 (dashboard) PHẢI gọi
 * {@link #summarize(YearMonth)} cho GET /reports/commissions-summary, KHÔNG tự query bảng
 * commissions (giữ đúng ARCHITECTURE.md: liên module chỉ qua Service interface public).
 */
public interface CommissionService {

    /**
     * Ghi nhận lương cho 1 buổi đã chốt (gọi bởi {@code BookingSettledCommissionListener}).
     * Tự tra {@code trainer_pay_rates} — KHÔNG nhận amount từ bên gọi, để "số tiền mỗi buổi"
     * chỉ có đúng 1 nguồn chân lý. Idempotent theo booking_id (gọi lại không tạo thêm bản ghi).
     * KHÔNG BAO GIỜ ném exception — PT chưa có mức lương thì bỏ qua và chỉ log cảnh báo, vì
     * listener chạy đồng bộ trong transaction completeBooking của Agent 3 (xem Javadoc listener).
     */
    void recordSessionPay(UUID trainerId, UUID bookingId, BookingSettledEvent.Outcome outcome);

    CommissionResponse markPaid(UUID commissionId);

    PageResponse<CommissionResponse> myCommissions(UUID trainerId, Pageable pageable);

    PageResponse<CommissionResponse> search(UUID staffId, YearMonth month, Pageable pageable);

    CommissionSummaryResponse summarize(YearMonth month);
}

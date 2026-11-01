package com.gym.management.service.listener;

import com.gym.management.service.CommissionService;
import com.gym.management.service.event.BookingSettledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Agent 5 - POS & Commission Module.
 *
 * Chạy SAU KHI transaction chốt buổi của Agent 3 đã commit ({@code AFTER_COMMIT}) và ghi lương
 * trong transaction RIÊNG ({@code REQUIRES_NEW} ở {@link CommissionService#recordSessionPay}).
 * Lý do (góp ý của Agent 1 ở PR #70): nếu chạy đồng bộ chung transaction với
 * {@code completeBooking}/{@code cancelBooking}/{@code applyLazyStatus}, một lỗi đi xuyên qua
 * ranh giới {@code @Transactional} (kể cả lỗi từ repository) đánh dấu transaction chung là
 * rollback-only trước khi tới {@code try/catch} bên dưới, nên buổi tập của PT vẫn bị rollback dù
 * lỗi đã được bắt. Tách transaction thì lỗi tính lương không thể ảnh hưởng việc chốt buổi.
 *
 * Đánh đổi: nếu ghi lương thất bại thì buổi vẫn chốt nhưng KHÔNG có bản ghi lương (chỉ có log
 * ERROR/WARN, cần bù thủ công). {@code fallbackExecution = true}: nếu có nơi phát event ngoài
 * transaction thì vẫn xử lý ngay thay vì bỏ rơi event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSettledCommissionListener {

    private final CommissionService commissionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingSettled(BookingSettledEvent event) {
        if (!event.fullDuration()) {
            log.info("Booking {} fullDuration=false — PT không dạy đủ thời gian và không nộp lý do, bỏ qua tính lương",
                    event.bookingId());
            return;
        }
        try {
            commissionService.recordSessionPay(event.trainerId(), event.bookingId(), event.outcome());
        } catch (Exception ex) {
            // Không bao giờ để lỗi tính lương làm hỏng transaction chốt buổi của PT.
            log.error("Lỗi khi ghi nhận lương cho booking {} (trainer {}, outcome {}) — bỏ qua, cần xử lý bù thủ công",
                    event.bookingId(), event.trainerId(), event.outcome(), ex);
        }
    }
}

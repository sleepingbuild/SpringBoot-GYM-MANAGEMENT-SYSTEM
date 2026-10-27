package com.gym.management.service.listener;

import com.gym.management.service.CommissionService;
import com.gym.management.service.event.BookingSettledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Agent 5 - POS & Commission Module.
 *
 * ⚠️ {@code @EventListener} đồng bộ — chạy ngay trong transaction của
 * {@code BookingServiceImpl#completeBooking}/{@code cancelBooking}/{@code applyLazyStatus}
 * (Agent 3), KHÔNG phải sau khi commit. Vì vậy method này TUYỆT ĐỐI không được ném
 * exception: một lỗi ở đây sẽ làm transaction hoàn thành/huỷ buổi của PT bị rollback theo
 * (xem docs/CHANGE_PT_ATTENDANCE_AND_PAY.md mục Agent 5 và cảnh báo thực tế Agent 2 đã gặp
 * ở AGENT2_REVIEW_REQUEST_PR_A.md mục 3). Toàn bộ logic nghiệp vụ (chống trùng, PT chưa có
 * mức lương) nằm trong {@link CommissionService#recordSessionPay}, bản thân nó cũng không
 * ném exception; try/catch dưới đây chỉ là lớp phòng thủ cuối cùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSettledCommissionListener {

    private final CommissionService commissionService;

    @EventListener
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

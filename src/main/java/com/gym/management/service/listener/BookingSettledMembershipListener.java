package com.gym.management.service.listener;

import com.gym.management.service.MembershipService;
import com.gym.management.service.MembershipService.ConsumeResult;
import com.gym.management.service.event.BookingSettledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Agent 2 - Membership, Package & Payment Module.
 *
 * Trừ 1 buổi của HỌC VIÊN khi 1 booking được "chốt" (xem {@link BookingSettledEvent} của Agent 3 và
 * CHANGE_PT_ATTENDANCE_AND_PAY.md). Cả 3 kết cục đều trừ 1 buổi:
 * COMPLETED, MEMBER_NO_SHOW (học viên vắng), LATE_CANCEL (huỷ muộn). Agent 3 KHÔNG phát sự kiện khi
 * PT_NO_SHOW hoặc CANCELLED nên các trường hợp đó không bao giờ tới đây.
 *
 * {@code fullDuration} CỐ Ý bị bỏ qua: nó chỉ quyết định PT có được trả công hay không (việc của Agent 5),
 * không liên quan tới việc học viên mất buổi.
 *
 * Listener chạy ĐỒNG BỘ, chung transaction với nơi phát (completeBooking/cancelBooking/applyLazyStatus của
 * Agent 3). Vì vậy dùng {@link MembershipService#tryConsumeSession} — method KHÔNG ném lỗi nghiệp vụ. Nếu
 * dùng method ném lỗi, transaction ngoài bị đánh dấu rollback-only và buổi tập không chốt được, PT mất công
 * chỉ vì lỗi kế toán gói của hội viên. Hội viên hết buổi / hết hạn chỉ được ghi cảnh báo để lễ tân xử lý.
 *
 * Lưu ý idempotency: không có cơ chế chống trừ trùng ở đây (khác Agent 5 có UNIQUE booking_id). Đúng khi
 * Agent 3 chỉ phát tối đa 1 sự kiện cho mỗi booking — hiện chốt buổi đi qua 1 lần chuyển trạng thái nên
 * đúng như vậy; nếu sau này có luồng phát lại sự kiện thì phải thêm ghi nhận theo booking_id ở đây.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSettledMembershipListener {

    private final MembershipService membershipService;

    @EventListener
    public void onBookingSettled(BookingSettledEvent event) {
        ConsumeResult result = membershipService.tryConsumeSession(event.memberId());

        if (result == ConsumeResult.NO_ACTIVE_MEMBERSHIP || result == ConsumeResult.NO_SESSION_LEFT) {
            log.warn("Khong tru duoc buoi cua hoi vien {} khi chot booking {} (outcome={}): {}",
                    event.memberId(), event.bookingId(), event.outcome(), result);
        }
    }
}

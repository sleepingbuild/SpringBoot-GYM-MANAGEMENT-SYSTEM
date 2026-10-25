package com.gym.management.service.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phát ra khi 1 Booking được "chốt" theo 1 trong 3 kết cục có ảnh hưởng tới tiền và số buổi
 * (thay thế BookingCompletedEvent cũ — xem CHANGE_PT_ATTENDANCE_AND_PAY.md).
 *
 * <ul>
 *   <li>{@code COMPLETED}: PT bấm hoàn thành ({@code completeBooking}).</li>
 *   <li>{@code MEMBER_NO_SHOW}: PT đã quét mặt vào, học viên không đến — lazy-flip sang NO_SHOW.</li>
 *   <li>{@code LATE_CANCEL}: học viên huỷ muộn (CONFIRMED, dưới 2 giờ) — LATE_CANCELLED.</li>
 * </ul>
 *
 * Cả 3 đều trừ 1 buổi của học viên (Agent 2) và PT được trả công (Agent 5), riêng
 * {@code fullDuration == false} thì Agent 5 bỏ qua. {@code fullDuration} luôn là true với
 * MEMBER_NO_SHOW và LATE_CANCEL (PT không có lỗi nên không bị trừ lương).
 *
 * Không phát khi PT_NO_SHOW (lỗi của PT: không trừ buổi, không trả công) hay CANCELLED.
 * Listener là {@code @EventListener} đồng bộ, chạy chung transaction với nơi phát.
 */
public record BookingSettledEvent(
        UUID bookingId,
        UUID trainerId,
        UUID memberId,
        Outcome outcome,
        boolean fullDuration,
        LocalDateTime settledAt
) {
    public enum Outcome {
        COMPLETED,
        MEMBER_NO_SHOW,
        LATE_CANCEL
    }
}

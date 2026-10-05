package com.gym.management.service.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phát ra khi 1 Booking chuyển sang COMPLETED (POST /bookings/{id}/complete).
 * Agent 5 (Commission) lắng nghe event này bằng @EventListener/@TransactionalEventListener
 * để tính hoa hồng PT theo commission_rate — KHÔNG cần Agent 3 gọi trực tiếp sang module
 * Agent 5, giữ đúng nguyên tắc ranh giới rõ ràng giữa các agent (ARCHITECTURE.md mục 1).
 *
 * ⚠️ CẦN AGENT 5 XÁC NHẬN: có dùng được cơ chế Spring event này không, hay muốn Agent 3
 * expose thêm 1 query method public (vd BookingService.findCompletedSince(...)) để họ tự poll.
 * Xem AGENT3_README.md mục 3.
 */
public record BookingCompletedEvent(
        UUID bookingId,
        UUID trainerId,
        UUID memberId,
        LocalDateTime completedAt
) {
}

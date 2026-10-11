package com.gym.management.entity;

/**
 * Trạng thái của 1 buổi đặt lịch PT.
 * Xem REQUIREMENTS.md mục 3 — auto-status là lazy-check, không cron.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
    PT_NO_SHOW,
    /** Học viên huỷ muộn (booking CONFIRMED, còn dưới 2 giờ): mất 1 buổi, PT vẫn được trả công. */
    LATE_CANCELLED
}

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
    PT_NO_SHOW
}

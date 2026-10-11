package com.gym.management.dto.response;

/**
 * Kết quả 1 lượt PT quét mặt, do {@code BookingService.recordTrainerFaceScan} trả về cho Agent 4.
 *
 * @param booking    booking vừa được cập nhật
 * @param type       {@link #CHECK_IN} hoặc {@link #CHECK_OUT}
 * @param earlyLeave chỉ có nghĩa khi CHECK_OUT: true nếu giờ quét nhỏ hơn giờ kết thúc buổi
 *                   (FE cần hiện ô nhập lý do, nộp qua PUT /bookings/{id}/early-leave-reason)
 */
public record TrainerScanResult(BookingResponse booking, String type, boolean earlyLeave) {

    public static final String CHECK_IN = "CHECK_IN";
    public static final String CHECK_OUT = "CHECK_OUT";
}

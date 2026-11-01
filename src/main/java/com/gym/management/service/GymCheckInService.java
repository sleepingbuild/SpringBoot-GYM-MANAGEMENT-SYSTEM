package com.gym.management.service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ghi lượt vào/ra phòng tập của hội viên khi quét mặt mà không có buổi PT phù hợp (CH2).
 */
public interface GymCheckInService {

    String ACTION_CHECK_IN = "CHECK_IN";
    String ACTION_CHECK_OUT = "CHECK_OUT";

    record ScanResult(String action, UUID checkInId) {
    }

    /**
     * Còn một lượt đang mở trong cùng ngày thì ghi check-out; ngược lại tạo lượt vào mới.
     * Chỉ khi tạo lượt vào mới mới yêu cầu hội viên có gói ACTIVE.
     *
     * @throws com.gym.management.exception.BusinessException (MEMBERSHIP_EXPIRED) nếu tạo lượt
     *         vào mới mà hội viên không có gói ACTIVE
     */
    ScanResult recordFaceScan(UUID memberId, LocalDateTime now);
}

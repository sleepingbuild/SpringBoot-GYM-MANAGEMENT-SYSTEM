package com.gym.management.constant;

import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Agent 6 - Trạng thái của lead và bảng chuyển trạng thái hợp lệ (một nguồn duy nhất).
 * CONVERTED chỉ được đặt qua POST /leads/{id}/convert, không qua API đổi trạng thái thường.
 */
public enum LeadStatus {
    NEW,
    CONTACTED,
    CONVERTED,
    LOST;

    private static final Map<LeadStatus, Set<LeadStatus>> ALLOWED = new EnumMap<>(LeadStatus.class);

    static {
        ALLOWED.put(NEW, EnumSet.of(CONTACTED, LOST, CONVERTED));
        ALLOWED.put(CONTACTED, EnumSet.of(LOST, CONVERTED));
        ALLOWED.put(LOST, EnumSet.of(CONTACTED));   // mở lại lead đã mất
        ALLOWED.put(CONVERTED, EnumSet.noneOf(LeadStatus.class)); // không chuyển ngược
    }

    public boolean canTransitionTo(LeadStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public static LeadStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Trạng thái lead không được để trống");
        }
        try {
            return LeadStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Trạng thái lead không hợp lệ: " + raw + " (NEW, CONTACTED, CONVERTED, LOST)");
        }
    }
}

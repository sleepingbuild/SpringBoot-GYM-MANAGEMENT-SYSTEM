package com.gym.management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Một lượt vào phòng tập của hội viên (M17). Hợp của hai nguồn loại trừ nhau:
 * `gym_check_ins` (lượt không có PT, type = GYM_VISIT) và `bookings` (buổi có PT mà hội viên
 * đã quét vào, type = PT_SESSION).
 */
@Getter
@Builder
public class CheckInHistoryItem {

    public static final String TYPE_GYM_VISIT = "GYM_VISIT";
    public static final String TYPE_PT_SESSION = "PT_SESSION";

    private String type;
    /** id của dòng gym_check_ins (GYM_VISIT) hoặc của booking (PT_SESSION). */
    private UUID id;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime; // null = chưa quét ra
    private String method;              // FACE / MANUAL
    private String trainerName;         // chỉ có khi type = PT_SESSION
}

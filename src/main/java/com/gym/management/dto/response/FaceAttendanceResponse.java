package com.gym.management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FaceAttendanceResponse {
    private UUID matchedUserId;
    private String matchedUserName;
    private String action;         // "CHECK_IN" / "CHECK_OUT" / "NONE"
    private String targetType;     // "BOOKING" / "STAFF_ATTENDANCE" / null
    private LocalDateTime scanTime;
    private boolean bookingLinked;
    private boolean duplicateScan; // true nếu bị chặn do quét lại quá sớm (quyết định A.3)
}

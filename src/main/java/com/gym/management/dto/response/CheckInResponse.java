package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CheckInResponse {
    private UUID checkInId;
    private UUID userId;
    private String fullName;
    private String method;       // QR, CARD, FACE
    private String status;       // SUCCESS, DENIED_...
    private Instant checkinTime;
    private Integer remainingSessions; // null nếu gói TIME_BASED thuần
}

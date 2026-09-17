package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class FaceProfileResponse {
    private UUID userId;
    private String fullName;
    private boolean registered;
    private Instant registeredAt;
}

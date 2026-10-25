package com.gym.management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FaceProfileResponse {
    private UUID id;
    private UUID userId;
    private String userFullName;
    private String referenceImage; // v1: luôn null (quyết định A.4)
    private UUID registeredBy;
    private LocalDateTime createdAt;
}

package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProfileResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private Integer age;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private String goal;
    private String avatarUrl;
}

package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private Integer sessionCount;
    private String packageType;
    private String peakType;
    private Integer maxSessionsPerWeek;
    private boolean active;
}

package com.gym.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPackageResponse {
    private UUID id;
    private UUID userId;
    private String userFullName;
    private UUID packageId;
    private String packageName;
    private UUID branchId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer remainingSessions;
}

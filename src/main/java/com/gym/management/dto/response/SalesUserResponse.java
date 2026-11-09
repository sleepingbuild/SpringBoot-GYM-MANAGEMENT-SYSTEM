package com.gym.management.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/** Agent 6 - Sales đang hoạt động, để Super Admin chọn người phụ trách lead. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalesUserResponse(UUID id, String fullName, String phone) {
}

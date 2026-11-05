package com.gym.management.dto.response;

import java.util.UUID;

/** Thông tin công khai của một huấn luyện viên (không lộ email hay số điện thoại). */
public record TrainerResponse(UUID id, String fullName, String avatarUrl) {
}

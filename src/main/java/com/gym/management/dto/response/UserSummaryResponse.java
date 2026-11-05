package com.gym.management.dto.response;

import java.util.UUID;

/** Thông tin tối thiểu của một hội viên để lễ tân/quản trị viên chọn (không chứa mật khẩu hay vai trò). */
public record UserSummaryResponse(UUID id, String fullName, String phone, String email) {
}

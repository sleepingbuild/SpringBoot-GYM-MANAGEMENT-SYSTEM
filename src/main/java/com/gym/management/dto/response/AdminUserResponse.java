package com.gym.management.dto.response;

import java.util.List;
import java.util.UUID;

/** Tài khoản hiển thị cho quản trị viên (không chứa mật khẩu). roles đã sắp theo tên. */
public record AdminUserResponse(UUID id, String fullName, String email, String phone, String status,
                                List<String> roles) {
}

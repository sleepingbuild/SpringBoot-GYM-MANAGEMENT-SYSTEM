package com.gym.management.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Trạng thái đăng ký khuôn mặt của chính người dùng. KHÔNG chứa descriptor.
 * Chưa đăng ký thì registered = false và updatedAt = null.
 */
@Getter
@Builder
public class FaceProfileStatusResponse {
    private boolean registered;
    private LocalDateTime updatedAt;
}

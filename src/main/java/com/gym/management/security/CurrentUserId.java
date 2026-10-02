package com.gym.management.security;

import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentUserId {

    private CurrentUserId() {
    }

    public static UUID get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof GymUserDetails details) {
            return details.getId();
        }
        throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Chưa đăng nhập hoặc token không hợp lệ");
    }
}
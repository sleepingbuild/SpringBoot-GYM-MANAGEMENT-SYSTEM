package com.gym.management.security;

import com.gym.management.entity.RoleName;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

public final class CurrentUserRole {

    private CurrentUserRole() {
    }

    public static String get() {
        Set<String> owned = ownedRoles();
        for (RoleName role : RoleName.values()) {
            if (owned.contains(role.name())) {
                return role.name();
            }
        }
        throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Tài khoản chưa được gán vai trò");
    }

    public static boolean has(String roleName) {
        return ownedRoles().contains(roleName);
    }

    private static Set<String> ownedRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Chưa đăng nhập hoặc token không hợp lệ");
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet());
    }
}
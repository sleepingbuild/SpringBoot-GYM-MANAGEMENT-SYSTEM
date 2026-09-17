package com.gym.management.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent 1 - SHARED FILE.
 * Gắn vào tham số controller để tự động lấy UUID của user đang đăng nhập
 * (parse từ JWT trong SecurityContext), tránh mỗi agent phải tự viết lại
 * logic "lấy email từ Authentication rồi query User".
 *
 * Ví dụ: public ApiResponse<X> foo(@CurrentUser UUID currentUserId) { ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {
}

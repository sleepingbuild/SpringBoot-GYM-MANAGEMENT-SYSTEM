package com.gym.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.exception.ApiResponse;
import com.gym.management.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Agent 1 - SHARED FILE.
 * Đã đăng nhập nhưng bị từ chối ở tầng đường dẫn của SecurityConfig (không phải @PreAuthorize):
 * trả 403 kèm ApiResponse JSON (errorCode AUTH_FORBIDDEN_ROLE), cùng dạng với lỗi do GlobalExceptionHandler trả.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("Bạn không có quyền thực hiện thao tác này", ErrorCode.AUTH_FORBIDDEN_ROLE));
    }
}

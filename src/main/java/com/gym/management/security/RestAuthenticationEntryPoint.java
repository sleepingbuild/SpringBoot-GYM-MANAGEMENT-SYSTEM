package com.gym.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.exception.ApiResponse;
import com.gym.management.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Agent 1 - SHARED FILE.
 * Người chưa đăng nhập (không có token, token sai hoặc hết hạn) gọi endpoint cần xác thực:
 * trả 401 kèm ApiResponse JSON (errorCode AUTH_TOKEN_INVALID) thay vì 403 body rỗng mặc định của Spring Security.
 * Frontend dựa vào errorCode này để làm mới token hoặc chuyển về trang đăng nhập.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("Chưa đăng nhập hoặc phiên đã hết hạn", ErrorCode.AUTH_TOKEN_INVALID));
    }
}

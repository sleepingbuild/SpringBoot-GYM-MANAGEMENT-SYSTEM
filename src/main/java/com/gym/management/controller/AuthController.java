package com.gym.management.controller;

import com.gym.management.dto.request.LoginRequest;
import com.gym.management.dto.request.RegisterRequest;
import com.gym.management.dto.response.AuthResponse;
import com.gym.management.exception.ApiResponse;
import com.gym.management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 1.
 * Endpoint public: /api/v1/auth/** (xem SecurityConfig.PUBLIC_ENDPOINTS).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Đăng ký / đăng nhập / làm mới token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản hội viên mới")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Đăng ký thành công", authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Đăng nhập thành công", authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token bằng refresh token")
    public ApiResponse<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ApiResponse.success(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất - vô hiệu hoá access token hiện tại")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ApiResponse.success("Đăng xuất thành công", null);
    }
}

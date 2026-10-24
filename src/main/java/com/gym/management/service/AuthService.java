package com.gym.management.service;

import com.gym.management.dto.request.LoginRequest;
import com.gym.management.dto.request.RegisterRequest;
import com.gym.management.dto.response.AuthResponse;
import com.gym.management.entity.User;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    void logout(String accessToken);

    /**
     * Tạo tài khoản hội viên (role MEMBER) KHÔNG đăng nhập và KHÔNG phát token.
     * Dành cho các module khác (vd: Leads chuyển lead thành hội viên).
     * Chuẩn hoá phone (trim, chuỗi rỗng thành null) và kiểm tra trùng email/phone.
     *
     * @throws com.gym.management.exception.BusinessException
     *         AUTH_EMAIL_ALREADY_EXISTS hoặc AUTH_PHONE_ALREADY_EXISTS khi trùng
     */
    User createMemberAccount(String fullName, String email, String phone, String rawPassword);
}

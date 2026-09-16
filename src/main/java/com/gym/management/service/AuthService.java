package com.gym.management.service;

import com.gym.management.dto.request.LoginRequest;
import com.gym.management.dto.request.RegisterRequest;
import com.gym.management.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    void logout(String accessToken);
}

package com.gym.management.service.impl;

import com.gym.management.dto.request.LoginRequest;
import com.gym.management.dto.request.RegisterRequest;
import com.gym.management.dto.response.AuthResponse;
import com.gym.management.entity.Role;
import com.gym.management.entity.RoleName;
import com.gym.management.entity.User;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.RoleRepository;
import com.gym.management.repository.UserRepository;
import com.gym.management.security.JwtTokenProvider;
import com.gym.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS, "Email đã được sử dụng");
        }

        Role memberRole = roleRepository.findByName(RoleName.MEMBER.name())
                .orElseThrow(() -> new IllegalStateException(
                        "Role MEMBER chưa được seed trong DB - kiểm tra V1__init_schema.sql"));

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(memberRole));
        userRepository.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of());
        return buildAuthResponse(user, authentication);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        return buildAuthResponse(user, authentication);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of());
        return buildAuthResponse(user, authentication);
    }

    @Override
    public void logout(String accessToken) {
        // TODO(Agent 1): đưa accessToken vào Redis blacklist với TTL = thời gian còn lại của token,
        // để JwtAuthenticationFilter từ chối token này dù chưa hết hạn tự nhiên.
    }

    private AuthResponse buildAuthResponse(User user, Authentication authentication) {
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getFullName(), roleNames);
    }
}

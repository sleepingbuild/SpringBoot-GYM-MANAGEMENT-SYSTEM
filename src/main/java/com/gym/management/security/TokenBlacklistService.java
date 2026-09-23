package com.gym.management.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Agent 1 - SHARED FILE.
 * Khi logout, access token được đưa vào Redis với TTL = thời gian còn lại của token,
 * để JwtAuthenticationFilter từ chối token này dù chưa hết hạn tự nhiên theo JWT.
 * Dùng chính chuỗi token làm key (đủ ngẫu nhiên/duy nhất, không cần hash thêm cho quy mô đồ án).
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String token, Instant expiresAt) {
        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (ttlSeconds <= 0) {
            return; // token đã hết hạn tự nhiên, không cần lưu
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + token, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}

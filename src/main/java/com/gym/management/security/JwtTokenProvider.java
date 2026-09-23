package com.gym.management.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Agent 1 - SHARED FILE.
 * Sinh & xác thực JWT (access token + refresh token).
 * Access token hết hạn ngắn (mặc định 15 phút); khi logout, token được đưa
 * vào Redis blacklist tới khi hết hạn tự nhiên (xem TokenBlacklistService).
 */
@Component
public class JwtTokenProvider {

    @Value("${gms.jwt.secret}")
    private String jwtSecret;

    @Value("${gms.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${gms.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Authentication authentication) {
        return buildToken(authentication.getName(), accessTokenExpirationMs);
    }

    public String generateRefreshToken(Authentication authentication) {
        return buildToken(authentication.getName(), refreshTokenExpirationMs);
    }

    private String buildToken(String subject, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /** Trả về Instant hết hạn của token — dùng để tính TTL khi đưa vào Redis blacklist lúc logout. */
    public java.time.Instant getExpirationFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration().toInstant();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

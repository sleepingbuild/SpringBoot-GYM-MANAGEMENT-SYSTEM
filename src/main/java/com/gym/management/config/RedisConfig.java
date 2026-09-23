package com.gym.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Agent 1 - SHARED FILE.
 * Redis dùng cho: JWT blacklist (logout), rate-limit (đăng nhập/face-attendance),
 * và bất kỳ agent nào cần cache tạm thời. Dùng StringRedisTemplate (key/value dạng
 * String) — đủ cho các nhu cầu trên, không cần serializer phức tạp.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}

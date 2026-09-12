package com.nortidart.selfmark.auth.service;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
    private static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redis;

    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void blacklist(String jti, long remainingSeconds) {
        if (jti == null || jti.isBlank() || remainingSeconds <= 0) {
            return;
        }
        redis.opsForValue().set(key(jti), "1", Duration.ofSeconds(remainingSeconds));
    }

    public boolean isBlacklisted(String jti) {
        return jti != null && !jti.isBlank() && Boolean.TRUE.equals(redis.hasKey(key(jti)));
    }

    public String key(String jti) {
        return KEY_PREFIX + jti;
    }
}

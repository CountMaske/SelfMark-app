package com.nortidart.selfmark.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTests {
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;

    @Test
    void storesOnlyJtiWithRemainingTokenTtl() {
        TokenBlacklistService service = new TokenBlacklistService(redis);
        when(redis.opsForValue()).thenReturn(values);

        service.blacklist("jti-1", 42);

        verify(values).set(eq("blacklist:jti-1"), eq("1"), eq(Duration.ofSeconds(42)));
    }

    @Test
    void checksRedisKeyAndDoesNotUseAnotherStore() {
        when(redis.hasKey("blacklist:jti-1")).thenReturn(true);

        assertThat(new TokenBlacklistService(redis).isBlacklisted("jti-1")).isTrue();
        verify(redis).hasKey("blacklist:jti-1");
    }
}

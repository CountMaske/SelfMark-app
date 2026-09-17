package com.nortidart.selfmark.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redis;

    // Redis 不可用时的本地降级存储：jti -> 过期时间戳（Epoch 秒）
    // 仅适用于单实例本地开发；多实例部署必须使用 Redis
    private final ConcurrentHashMap<String, Long> localBlacklist = new ConcurrentHashMap<>();
    // 触发全量清扫的条目数阈值：超过后在下次登出时清理所有已过期条目
    private static final int SWEEP_THRESHOLD = 1024;
    // Redis 是否可用：一旦连接失败就切到内存模式，避免每个请求都等连接超时
    private volatile boolean redisAvailable = true;

    public TokenBlacklistService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void blacklist(String jti, long remainingSeconds) {
        if (jti == null || jti.isBlank() || remainingSeconds <= 0) {
            return;
        }
        long expireAt = Instant.now().getEpochSecond() + remainingSeconds;
        if (!redisAvailable) {
            localBlacklist.put(jti, expireAt);
            sweepIfNeeded(); // 条目堆积到阈值时清扫过期数据，给内存占用设上限
            return;
        }
        try {
            redis.opsForValue().set(key(jti), "1", Duration.ofSeconds(remainingSeconds));
        } catch (RedisConnectionFailureException e) {
            switchToLocal(e);
            localBlacklist.put(jti, expireAt);
            sweepIfNeeded();
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        if (!redisAvailable) {
            return isLocalBlacklisted(jti);
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(key(jti)));
        } catch (RedisConnectionFailureException e) {
            switchToLocal(e);
            return isLocalBlacklisted(jti);
        }
    }

    // 检查内存黑名单，顺带清理已过期的条目
    private boolean isLocalBlacklisted(String jti) {
        Long expireAt = localBlacklist.get(jti);
        if (expireAt == null) {
            return false;
        }
        if (expireAt <= Instant.now().getEpochSecond()) {
            localBlacklist.remove(jti);
            return false;
        }
        return true;
    }

    // 条目数超过阈值时，全量清理已过期条目。
    // 登出是低频操作，O(n) 遍历代价可忽略；清扫后 Map 中只剩未过期条目，
    // 因此内存上限 ≈ 阈值 + 两次登出之间的新增量（约 1024 条 × 150B ≈ 150KB）。
    private void sweepIfNeeded() {
        if (localBlacklist.size() < SWEEP_THRESHOLD) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        localBlacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    // 只在第一次失败时打一条警告，避免日志被刷屏
    private void switchToLocal(RedisConnectionFailureException e) {
        if (redisAvailable) {
            log.warn("Redis 不可用，token 黑名单降级为内存模式（仅限单实例本地开发）：{}", e.getMessage());
            redisAvailable = false;
        }
    }

    public String key(String jti) {
        return KEY_PREFIX + jti;
    }
}

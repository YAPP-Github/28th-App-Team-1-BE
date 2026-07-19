package com.yapp.d14.jd.adapter.out.cache;

import com.yapp.d14.jd.application.port.out.JdValidationRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class RedisJdValidationRateLimiterAdapter implements JdValidationRateLimiter {

    private static final String KEY_PREFIX = "jd:rate:";
    private static final Duration TTL = Duration.ofHours(25);
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redisTemplate;

    @Override
    public int getTodayCount(UUID userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Override
    public void increment(UUID userId) {
        String key = key(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, TTL);
        }
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now(ZONE);
    }
}

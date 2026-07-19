package com.yapp.d14.jd.adapter.out.cache;

import com.yapp.d14.jd.application.port.out.JdValidationRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class RedisJdValidationRateLimiterAdapter implements JdValidationRateLimiter {

    private static final String KEY_PREFIX = "jd:rate:";
    private static final Duration TTL = Duration.ofHours(25);
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    // INCR과 EXPIRE를 하나의 스크립트로 묶어 원자적으로 실행 (TTL 유실 방지)
    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public int getTodayCount(UUID userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Override
    public void increment(UUID userId) {
        redisTemplate.execute(INCREMENT_SCRIPT, Collections.singletonList(key(userId)), String.valueOf(TTL.toSeconds()));
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId + ":" + LocalDate.now(ZONE);
    }
}

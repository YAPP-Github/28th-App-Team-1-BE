package com.yapp.d14.jd.adapter.out.cache;

import com.yapp.d14.jd.application.port.out.JdContentCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class RedisJdContentCacheAdapter implements JdContentCache {

    private static final String KEY_PREFIX = "jd:";
    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(UUID userId, String jdUrl, String content) {
        redisTemplate.opsForValue().set(key(userId, jdUrl), content, TTL);
    }

    private String key(UUID userId, String jdUrl) {
        return KEY_PREFIX + userId + ":" + jdUrl;
    }
}

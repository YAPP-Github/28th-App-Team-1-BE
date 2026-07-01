package com.yapp.d14.auth.adapter.out.cache;

import com.yapp.d14.auth.application.port.out.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class RedisTokenAdapter implements TokenRepository {

    private static final String KEY_PREFIX = "refresh:token:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(UUID userId, String refreshToken) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, TTL);
    }

    @Override
    public Optional<String> find(UUID userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
    }

    @Override
    public void delete(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}

package com.yapp.d14.interview.adapter.out.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yapp.d14.interview.application.port.out.JdOpenerContext;
import com.yapp.d14.interview.application.port.out.JdOpenerContextCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

// 조건부 opener(핵심 항목이 비었을 때 JD∩포폴 키워드로 여는 질문)의 소재 캐시.
// 키: session:{sessionId}:jd-opener-context, 값: JdOpenerContext를 JSON으로 직렬화.
@Slf4j
@Repository
@RequiredArgsConstructor
class RedisJdOpenerContextAdapter implements JdOpenerContextCache {

    private static final String KEY_PREFIX = "session:";
    private static final String KEY_SUFFIX = ":jd-opener-context";
    // 명시적 clear() 호출 누락에 대비한 안전망 TTL (세션 최대 진행 시간보다 넉넉하게)
    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<JdOpenerContext> get(Long sessionId) {
        String raw = redisTemplate.opsForValue().get(key(sessionId));
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(raw));
    }

    @Override
    public void save(Long sessionId, JdOpenerContext context) {
        String key = key(sessionId);
        redisTemplate.opsForValue().set(key, serialize(context));
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void clear(Long sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private String key(Long sessionId) {
        return KEY_PREFIX + sessionId + KEY_SUFFIX;
    }

    private String serialize(JdOpenerContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JdOpenerContext 직렬화에 실패했어요.", e);
        }
    }

    private JdOpenerContext deserialize(String raw) {
        try {
            return objectMapper.readValue(raw, JdOpenerContext.class);
        } catch (JsonProcessingException e) {
            log.error("[JD OPENER CONTEXT CACHE] 역직렬화 실패: payloadLength={}", raw.length(), e);
            throw new IllegalStateException("JdOpenerContext 역직렬화에 실패했어요.", e);
        }
    }
}

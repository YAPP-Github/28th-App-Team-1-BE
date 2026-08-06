package com.yapp.d14.interview.adapter.out.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yapp.d14.interview.application.port.out.PriorQuestionCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

// 이전 면접에서 출제된 질문 원문 캐시. 여는 질문 생성이 라이브 턴마다 호출되므로 preload에서 한 번 조회해 담아둔다.
// 키: session:{sessionId}:prior-questions, 값: 질문 문자열 배열 JSON.
@Slf4j
@Repository
@RequiredArgsConstructor
class RedisPriorQuestionAdapter implements PriorQuestionCache {

    private static final String KEY_PREFIX = "session:";
    private static final String KEY_SUFFIX = ":prior-questions";
    // 명시적 clear() 호출 누락에 대비한 안전망 TTL (세션 최대 진행 시간보다 넉넉하게)
    private static final Duration TTL = Duration.ofHours(6);
    private static final TypeReference<List<String>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<List<String>> get(Long sessionId) {
        String raw = redisTemplate.opsForValue().get(key(sessionId));
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.of(deserialize(raw));
    }

    @Override
    public void save(Long sessionId, List<String> priorQuestions) {
        String key = key(sessionId);
        redisTemplate.opsForValue().set(key, serialize(priorQuestions));
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void clear(Long sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private String key(Long sessionId) {
        return KEY_PREFIX + sessionId + KEY_SUFFIX;
    }

    private String serialize(List<String> priorQuestions) {
        try {
            return objectMapper.writeValueAsString(priorQuestions);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이전 질문 이력 직렬화에 실패했어요.", e);
        }
    }

    private List<String> deserialize(String raw) {
        try {
            return objectMapper.readValue(raw, PAYLOAD_TYPE);
        } catch (JsonProcessingException e) {
            log.error("[PRIOR QUESTION CACHE] 역직렬화 실패: payloadLength={}", raw.length(), e);
            throw new IllegalStateException("이전 질문 이력 역직렬화에 실패했어요.", e);
        }
    }
}

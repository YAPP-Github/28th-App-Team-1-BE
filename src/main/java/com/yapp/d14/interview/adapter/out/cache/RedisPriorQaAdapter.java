package com.yapp.d14.interview.adapter.out.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yapp.d14.interview.application.port.out.PriorQaCache;
import com.yapp.d14.interview.application.port.out.PriorTurn;
import com.yapp.d14.interview.domain.TestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// run_live_turn의 prior_qa/get_prior_qa가 쓰는 axis별 이전 턴 이력 캐시 (3단계 4-1장).
// 키: session:{sessionId}:axis:{axis}, 값: PriorTurn을 JSON으로 직렬화해 Redis List에 턴 순서대로 적재.
@Slf4j
@Repository
@RequiredArgsConstructor
class RedisPriorQaAdapter implements PriorQaCache {

    private static final String KEY_PREFIX = "session:";
    private static final String KEY_MIDDLE = ":axis:";
    // 명시적 clear() 호출 누락에 대비한 안전망 TTL (세션 최대 진행 시간보다 넉넉하게)
    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<PriorTurn> get(Long sessionId, TestType axis) {
        List<String> rawEntries = redisTemplate.opsForList().range(key(sessionId, axis), 0, -1);
        if (rawEntries == null) {
            return List.of();
        }
        return rawEntries.stream().map(this::deserialize).toList();
    }

    @Override
    public void append(Long sessionId, TestType axis, PriorTurn turn) {
        String key = key(sessionId, axis);
        redisTemplate.opsForList().rightPush(key, serialize(turn));
        redisTemplate.expire(key, TTL);
    }

    @Override
    public void clear(Long sessionId) {
        Set<String> keys = Arrays.stream(TestType.values())
                .map(axis -> key(sessionId, axis))
                .collect(Collectors.toSet());
        redisTemplate.delete(keys);
    }

    private String key(Long sessionId, TestType axis) {
        return KEY_PREFIX + sessionId + KEY_MIDDLE + axis.name().toLowerCase();
    }

    private String serialize(PriorTurn turn) {
        try {
            return objectMapper.writeValueAsString(turn);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("PriorTurn 직렬화에 실패했어요.", e);
        }
    }

    private PriorTurn deserialize(String raw) {
        try {
            return objectMapper.readValue(raw, PriorTurn.class);
        } catch (JsonProcessingException e) {
            log.error("[PRIOR QA CACHE] PriorTurn 역직렬화 실패, 해당 항목은 건너뜁니다: raw={}", raw, e);
            throw new IllegalStateException("PriorTurn 역직렬화에 실패했어요.", e);
        }
    }
}

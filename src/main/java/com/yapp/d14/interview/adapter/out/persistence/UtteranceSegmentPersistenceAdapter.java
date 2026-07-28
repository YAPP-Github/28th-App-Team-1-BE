package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.UtteranceSegmentJpaEntity;
import com.yapp.d14.interview.application.port.out.UtteranceSegmentRepository;
import com.yapp.d14.interview.domain.ScriptRole;
import com.yapp.d14.interview.domain.UtteranceSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
class UtteranceSegmentPersistenceAdapter implements UtteranceSegmentRepository {

    private final UtteranceSegmentJpaRepository utteranceSegmentJpaRepository;

    @Override
    public void saveAll(Long sessionId, Long questionId, List<UtteranceSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return;
        }
        List<UtteranceSegmentJpaEntity> entities = segments.stream()
                .map(segment -> UtteranceSegmentJpaEntity.from(sessionId, questionId, segment))
                .toList();
        utteranceSegmentJpaRepository.saveAll(entities);
    }

    @Override
    public Map<Long, List<UtteranceSegment>> findBySessionIdGroupedByQuestionId(Long sessionId) {
        // startSec 오름차순으로 읽어 questionId별로 묶는다 — 각 카드 안에서 질문 문장 → 답변 문장 순이 유지된다.
        return utteranceSegmentJpaRepository.findAllBySessionIdOrderByStartSecAsc(sessionId).stream()
                .collect(Collectors.groupingBy(
                        UtteranceSegmentJpaEntity::getQuestionId,
                        LinkedHashMap::new,
                        Collectors.mapping(UtteranceSegmentJpaEntity::toDomain, Collectors.toList())));
    }

    @Override
    @Transactional
    public void deleteBySessionIdAndRole(Long sessionId, ScriptRole role) {
        utteranceSegmentJpaRepository.deleteBySessionIdAndRole(sessionId, role);
    }
}

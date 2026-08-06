package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.QuestionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, Long> {

    List<QuestionJpaEntity> findAllBySessionId(Long sessionId);

    // turnLevel 0(요약 질문)은 LLM이 아니라 고정 템플릿으로 만들어지므로 중복 회피 대상이 아니다.
    @Query("SELECT q.content FROM QuestionJpaEntity q "
            + "WHERE q.sessionId IN :sessionIds AND q.turnLevel > 0 AND q.content IS NOT NULL "
            + "ORDER BY q.createdAt DESC")
    List<String> findRecentContentsBySessionIdIn(@Param("sessionIds") List<Long> sessionIds, Pageable pageable);

    Optional<QuestionJpaEntity> findBySessionIdAndTurnLevel(Long sessionId, Integer turnLevel);

    Optional<QuestionJpaEntity> findTopBySessionIdOrderByTurnLevelDesc(Long sessionId);

    void deleteAllBySessionId(Long sessionId);
}

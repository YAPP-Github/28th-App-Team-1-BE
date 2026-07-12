package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.QuestionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, Long> {

    List<QuestionJpaEntity> findAllBySessionId(Long sessionId);

    Optional<QuestionJpaEntity> findBySessionIdAndTurnLevel(Long sessionId, Integer turnLevel);
}

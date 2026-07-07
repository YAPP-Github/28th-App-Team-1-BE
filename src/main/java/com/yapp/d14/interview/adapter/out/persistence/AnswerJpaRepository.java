package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.AnswerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface AnswerJpaRepository extends JpaRepository<AnswerJpaEntity, Long> {

    List<AnswerJpaEntity> findAllBySessionId(Long sessionId);

    Optional<AnswerJpaEntity> findByQuestionId(Long questionId);
}

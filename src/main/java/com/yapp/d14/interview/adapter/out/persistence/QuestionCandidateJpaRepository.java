package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.QuestionCandidateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface QuestionCandidateJpaRepository extends JpaRepository<QuestionCandidateJpaEntity, Long> {

    List<QuestionCandidateJpaEntity> findAllBySessionId(Long sessionId);

    void deleteAllBySessionId(Long sessionId);
}

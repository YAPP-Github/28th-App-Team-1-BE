package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.QuestionCandidateJpaEntity;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.TestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface QuestionCandidateJpaRepository extends JpaRepository<QuestionCandidateJpaEntity, Long> {

    List<QuestionCandidateJpaEntity> findAllBySessionId(Long sessionId);

    List<QuestionCandidateJpaEntity> findAllBySessionIdAndTestTypeAndStatus(
            Long sessionId, TestType testType, QuestionCandidateStatus status);

    void deleteAllBySessionId(Long sessionId);
}

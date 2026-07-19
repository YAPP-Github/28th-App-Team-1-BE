package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.QuestionCandidateJpaEntity;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.TestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface QuestionCandidateJpaRepository extends JpaRepository<QuestionCandidateJpaEntity, Long> {

    List<QuestionCandidateJpaEntity> findAllBySessionId(Long sessionId);

    List<QuestionCandidateJpaEntity> findAllBySessionIdAndTestTypeAndStatus(
            Long sessionId, TestType testType, QuestionCandidateStatus status);

    // flushAutomatically: 같은 트랜잭션에서 방금 saveAll()한 신규 OPEN 후보를 이 벌크 UPDATE가 봐야 한다.
    // Hibernate 기본 FlushMode.AUTO가 실제로는 이미 처리해주지만(통합 테스트로 확인함), 이 정합성을
    // 특정 JPA 공급자의 암묵적 기본값에 맡기지 않고 명시적으로 고정해둔다.
    @Modifying(flushAutomatically = true)
    @Query("UPDATE QuestionCandidateJpaEntity c SET c.status = com.yapp.d14.interview.domain.QuestionCandidateStatus.EXHAUSTED "
            + "WHERE c.sessionId = :sessionId AND c.testType = :testType "
            + "AND c.status = com.yapp.d14.interview.domain.QuestionCandidateStatus.OPEN")
    void exhaustOpenBySessionIdAndTestType(@Param("sessionId") Long sessionId, @Param("testType") TestType testType);

    void deleteAllBySessionId(Long sessionId);
}

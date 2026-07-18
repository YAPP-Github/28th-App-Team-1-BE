package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewVideoJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface InterviewVideoJpaRepository extends JpaRepository<InterviewVideoJpaEntity, Long> {

    Optional<InterviewVideoJpaEntity> findBySessionId(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM InterviewVideoJpaEntity v WHERE v.sessionId = :sessionId")
    Optional<InterviewVideoJpaEntity> findBySessionIdForUpdate(@Param("sessionId") Long sessionId);
}

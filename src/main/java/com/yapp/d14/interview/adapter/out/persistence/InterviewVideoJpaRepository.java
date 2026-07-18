package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewVideoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface InterviewVideoJpaRepository extends JpaRepository<InterviewVideoJpaEntity, Long> {

    Optional<InterviewVideoJpaEntity> findBySessionId(Long sessionId);
}

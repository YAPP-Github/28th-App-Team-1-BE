package com.yapp.d14.feedback.adapter.out.persistence;

import com.yapp.d14.feedback.adapter.out.persistence.entity.FeedbackShareJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface FeedbackShareJpaRepository extends JpaRepository<FeedbackShareJpaEntity, Long> {

    Optional<FeedbackShareJpaEntity> findBySessionId(Long sessionId);

    Optional<FeedbackShareJpaEntity> findByToken(String token);
}

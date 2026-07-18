package com.yapp.d14.feedback.adapter.out.persistence;

import com.yapp.d14.feedback.adapter.out.persistence.entity.FeedbackShareJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface FeedbackShareJpaRepository extends JpaRepository<FeedbackShareJpaEntity, Long> {

    Optional<FeedbackShareJpaEntity> findBySessionId(Long sessionId);

    Optional<FeedbackShareJpaEntity> findByToken(String token);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FeedbackShareJpaEntity f SET f.status = 'PRIVATE' WHERE f.id = :id")
    void markPrivate(@Param("id") Long id);
}

package com.yapp.d14.feedback.adapter.out.persistence;

import com.yapp.d14.feedback.adapter.out.persistence.entity.FeedbackShareJpaEntity;
import com.yapp.d14.feedback.application.port.out.FeedbackShareRepository;
import com.yapp.d14.feedback.domain.FeedbackShare;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class FeedbackSharePersistenceAdapter implements FeedbackShareRepository {

    private final FeedbackShareJpaRepository feedbackShareJpaRepository;

    @Override
    public FeedbackShare save(FeedbackShare feedbackShare) {
        return feedbackShareJpaRepository.save(FeedbackShareJpaEntity.from(feedbackShare)).toDomain();
    }

    @Override
    public Optional<FeedbackShare> findBySessionId(Long sessionId) {
        return feedbackShareJpaRepository.findBySessionId(sessionId).map(FeedbackShareJpaEntity::toDomain);
    }

    @Override
    public Optional<FeedbackShare> findByToken(String token) {
        return feedbackShareJpaRepository.findByToken(token).map(FeedbackShareJpaEntity::toDomain);
    }

    @Override
    public Optional<FeedbackShare> findByTokenForUpdate(String token) {
        return feedbackShareJpaRepository.findByTokenForUpdate(token).map(FeedbackShareJpaEntity::toDomain);
    }

    @Override
    public void markPrivate(Long id) {
        feedbackShareJpaRepository.markPrivate(id);
    }
}

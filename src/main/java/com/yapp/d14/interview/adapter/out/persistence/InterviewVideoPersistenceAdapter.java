package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewVideoJpaEntity;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.domain.InterviewVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class InterviewVideoPersistenceAdapter implements InterviewVideoRepository {

    private final InterviewVideoJpaRepository interviewVideoJpaRepository;

    @Override
    public InterviewVideo save(InterviewVideo interviewVideo) {
        return interviewVideoJpaRepository.save(InterviewVideoJpaEntity.from(interviewVideo)).toDomain();
    }

    @Override
    public Optional<InterviewVideo> findBySessionId(Long sessionId) {
        return interviewVideoJpaRepository.findBySessionId(sessionId).map(InterviewVideoJpaEntity::toDomain);
    }

    @Override
    public Optional<InterviewVideo> findBySessionIdForUpdate(Long sessionId) {
        return interviewVideoJpaRepository.findBySessionIdForUpdate(sessionId).map(InterviewVideoJpaEntity::toDomain);
    }

    @Override
    public void insertRetentionIfAbsent(InterviewVideo v) {
        interviewVideoJpaRepository.insertRetentionIfAbsent(v.getSessionId(), v.getBaseAt(), v.getExpiresAt());
    }

    @Override
    public void upsertUploaded(InterviewVideo v) {
        interviewVideoJpaRepository.upsertUploaded(v.getSessionId(), v.getBaseAt(), v.getExpiresAt());
    }
}

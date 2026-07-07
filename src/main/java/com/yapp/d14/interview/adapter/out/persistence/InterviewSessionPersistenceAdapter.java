package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewSessionJpaEntity;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class InterviewSessionPersistenceAdapter implements InterviewSessionRepository {

    private final InterviewSessionJpaRepository interviewSessionJpaRepository;

    @Override
    public InterviewSession save(InterviewSession interviewSession) {
        return interviewSessionJpaRepository.save(InterviewSessionJpaEntity.from(interviewSession)).toDomain();
    }

    @Override
    public Optional<InterviewSession> findById(Long id) {
        return interviewSessionJpaRepository.findById(id).map(InterviewSessionJpaEntity::toDomain);
    }

    @Override
    public List<InterviewSession> findAllByUserId(UUID userId) {
        return interviewSessionJpaRepository.findAllByUserId(userId).stream()
                .map(InterviewSessionJpaEntity::toDomain)
                .toList();
    }
}

package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.InterviewAxisPlanJpaEntity;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class InterviewAxisPlanPersistenceAdapter implements InterviewAxisPlanRepository {

    private final InterviewAxisPlanJpaRepository interviewAxisPlanJpaRepository;

    @Override
    public InterviewAxisPlan save(InterviewAxisPlan interviewAxisPlan) {
        return interviewAxisPlanJpaRepository.save(InterviewAxisPlanJpaEntity.from(interviewAxisPlan)).toDomain();
    }

    @Override
    public Optional<InterviewAxisPlan> findById(Long id) {
        return interviewAxisPlanJpaRepository.findById(id).map(InterviewAxisPlanJpaEntity::toDomain);
    }

    @Override
    public List<InterviewAxisPlan> findAllBySessionId(Long sessionId) {
        return interviewAxisPlanJpaRepository.findAllBySessionId(sessionId).stream()
                .map(InterviewAxisPlanJpaEntity::toDomain)
                .toList();
    }
}

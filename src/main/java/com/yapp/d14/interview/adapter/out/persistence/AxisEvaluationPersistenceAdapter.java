package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.AxisEvaluationJpaEntity;
import com.yapp.d14.interview.application.port.out.AxisEvaluationRepository;
import com.yapp.d14.interview.domain.AxisEvaluation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class AxisEvaluationPersistenceAdapter implements AxisEvaluationRepository {

    private final AxisEvaluationJpaRepository axisEvaluationJpaRepository;

    @Override
    public AxisEvaluation save(AxisEvaluation axisEvaluation) {
        return axisEvaluationJpaRepository.save(AxisEvaluationJpaEntity.from(axisEvaluation)).toDomain();
    }

    @Override
    public List<AxisEvaluation> findAllBySessionId(Long sessionId) {
        return axisEvaluationJpaRepository.findAllBySessionId(sessionId).stream()
                .map(AxisEvaluationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        axisEvaluationJpaRepository.deleteBySessionId(sessionId);
    }
}

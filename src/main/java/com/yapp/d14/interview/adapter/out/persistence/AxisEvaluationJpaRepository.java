package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.AxisEvaluationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AxisEvaluationJpaRepository extends JpaRepository<AxisEvaluationJpaEntity, Long> {

    List<AxisEvaluationJpaEntity> findAllBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}

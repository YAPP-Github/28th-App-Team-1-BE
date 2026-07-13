package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.AxisEvaluation;

import java.util.List;

public interface AxisEvaluationRepository {

    AxisEvaluation save(AxisEvaluation axisEvaluation);

    List<AxisEvaluation> findAllBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}

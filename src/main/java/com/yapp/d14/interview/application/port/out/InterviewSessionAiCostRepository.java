package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.InterviewSessionAiCost;

import java.util.Optional;

public interface InterviewSessionAiCostRepository {

    void accumulate(AiCostDelta delta);

    Optional<InterviewSessionAiCost> findBySessionId(Long sessionId);
}

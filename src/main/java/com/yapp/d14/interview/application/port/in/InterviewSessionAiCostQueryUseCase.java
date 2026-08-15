package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.domain.InterviewSessionAiCost;

import java.util.Optional;

public interface InterviewSessionAiCostQueryUseCase {

    Optional<InterviewSessionAiCost> findBySessionId(Long sessionId);
}

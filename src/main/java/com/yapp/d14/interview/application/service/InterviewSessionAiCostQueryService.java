package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionAiCostQueryUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionAiCostRepository;
import com.yapp.d14.interview.domain.InterviewSessionAiCost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class InterviewSessionAiCostQueryService implements InterviewSessionAiCostQueryUseCase {

    private final InterviewSessionAiCostRepository interviewSessionAiCostRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<InterviewSessionAiCost> findBySessionId(Long sessionId) {
        return interviewSessionAiCostRepository.findBySessionId(sessionId);
    }
}

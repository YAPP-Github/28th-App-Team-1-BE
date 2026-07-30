package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionInProgressCheckUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewSessionInProgressCheckService implements InterviewSessionInProgressCheckUseCase {

    private final InterviewSessionRepository interviewSessionRepository;

    @Override
    public boolean existsInProgress(UUID portfolioId) {
        return interviewSessionRepository.existsByPortfolioIdAndStatus(portfolioId, InterviewSessionStatus.IN_PROGRESS);
    }
}

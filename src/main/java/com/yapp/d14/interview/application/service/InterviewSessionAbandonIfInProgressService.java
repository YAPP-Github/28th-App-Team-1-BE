package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionAbandonIfInProgressUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class InterviewSessionAbandonIfInProgressService implements InterviewSessionAbandonIfInProgressUseCase {

    private final InterviewSessionRepository interviewSessionRepository;

    @Override
    @Transactional
    public void abandon(Long sessionId, AbandonCause cause) {
        interviewSessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() == InterviewSessionStatus.IN_PROGRESS) {
                session.markAbandoned(cause);
                interviewSessionRepository.save(session);
            }
        });
    }
}

package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionAbandonOnHoldExpiryUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class InterviewSessionAbandonOnHoldExpiryService implements InterviewSessionAbandonOnHoldExpiryUseCase {

    private final InterviewSessionRepository interviewSessionRepository;

    @Override
    @Transactional
    public void abandonForHoldExpiry(Long sessionId) {
        interviewSessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getStatus() == InterviewSessionStatus.IN_PROGRESS) {
                session.markAbandoned(AbandonCause.HOLD_EXPIRED);
                interviewSessionRepository.save(session);
            }
        });
    }
}

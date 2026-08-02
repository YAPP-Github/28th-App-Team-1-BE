package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionAbandonIfInProgressUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
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
    public boolean abandon(Long sessionId, AbandonCause cause) {
        // 호출자(TicketAvailabilityCheckService 등)가 이 반환값으로 이용권 환급 여부를 결정하므로,
        // 조회-확인-갱신 사이 경합을 락으로 막는다(InterviewAbandonPersister와 동일 원칙).
        InterviewSession session = interviewSessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null || session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            return false;
        }
        session.markAbandoned(cause);
        interviewSessionRepository.save(session);
        return true;
    }
}

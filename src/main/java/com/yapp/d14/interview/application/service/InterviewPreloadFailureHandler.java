package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.ticket.application.port.in.TicketReleaseUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
class InterviewPreloadFailureHandler {

    private static final String OUTCOME_PRELOAD_FAILED = "PRELOAD_FAILED";

    private final InterviewSessionRepository interviewSessionRepository;
    private final TicketReleaseUseCase ticketReleaseUseCase;

    @Transactional
    void markFailed(Long sessionId) {
        InterviewSession session = interviewSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            log.warn("[INTERVIEW PRELOAD] 실패 처리할 세션을 찾을 수 없어요: sessionId={}", sessionId);
            return;
        }

        session.markPreloadFailed();
        interviewSessionRepository.save(session);
        ticketReleaseUseCase.release(sessionId, OUTCOME_PRELOAD_FAILED);
    }
}

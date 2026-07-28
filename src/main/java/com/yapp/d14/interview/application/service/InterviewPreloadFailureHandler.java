package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
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
    private final QuestionCandidateRepository questionCandidateRepository;
    private final QuestionRepository questionRepository;
    private final TicketReleaseUseCase ticketReleaseUseCase;

    @Transactional
    boolean markFailed(Long sessionId) {
        InterviewSession session = interviewSessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null) {
            log.warn("[INTERVIEW PRELOAD] 실패 처리할 세션을 찾을 수 없어요: sessionId={}", sessionId);
            return false;
        }
        if (session.getStatus() != InterviewSessionStatus.PREPARING) {
            log.warn("[INTERVIEW PRELOAD] 이미 처리된 세션이라 타임아웃 실패 처리를 건너뜁니다: sessionId={}, currentStatus={}",
                    sessionId, session.getStatus());
            return false;
        }

        questionCandidateRepository.deleteBySessionId(sessionId);
        questionRepository.deleteBySessionId(sessionId);

        session.markPreloadFailed();
        interviewSessionRepository.save(session);

        try {
            ticketReleaseUseCase.release(sessionId, OUTCOME_PRELOAD_FAILED);
        } catch (Exception e) {
            log.error("[INTERVIEW PRELOAD] 이용권 release 실패, 세션은 FAILED로 유지: sessionId={}", sessionId, e);
        }
        return true;
    }
}

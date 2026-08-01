package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.ticket.application.port.in.TicketCommitUseCase;
import com.yapp.d14.ticket.application.port.in.TicketReleaseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.RejectedExecutionException;

@Component
@RequiredArgsConstructor
class InterviewAbandonPersister {

    private final InterviewSessionRepository interviewSessionRepository;
    private final TicketCommitUseCase ticketCommitUseCase;
    private final TicketReleaseUseCase ticketReleaseUseCase;
    private final InterviewReportGenerateUseCase interviewReportGenerateUseCase;
    private final InterviewReportFailureHandler interviewReportFailureHandler;

    record PersistResult(LocalDateTime endedAt, String ticketOutcome, boolean reportGenerating) {
    }

    @Transactional
    PersistResult persist(InterviewSession session, AbandonCause cause) {
        session.markAbandoned(cause);
        interviewSessionRepository.save(session);

        // 이용권 처리 실패 시 트랜잭션 전체를 롤백한다(try/catch로 삼키지 않음) — InterviewSttResetPersister와 동일 원칙.
        if (cause == AbandonCause.USER_EXIT) {
            ticketCommitUseCase.commit(session.getId(), cause.name());
            triggerReportGeneration(session.getId());
            return new PersistResult(session.getEndedAt(), "COMMITTED", true);
        }

        ticketReleaseUseCase.release(session.getId(), cause.name());
        return new PersistResult(session.getEndedAt(), "RELEASED", false);
    }

    private void triggerReportGeneration(Long sessionId) {
        try {
            interviewReportGenerateUseCase.generate(sessionId);
        } catch (RejectedExecutionException e) {
            interviewReportFailureHandler.markFailed(sessionId);
        }
    }
}

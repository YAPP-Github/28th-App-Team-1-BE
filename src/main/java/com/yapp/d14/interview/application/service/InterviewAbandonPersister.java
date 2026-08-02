package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
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
        // 서비스 레이어의 상태 확인과 이 저장 사이에 다른 요청(중복 중단·답변 제출로 인한 종료 등)이 끼어들 수 있다 —
        // InterviewPreloadResultPersister와 동일하게 락 안에서 IN_PROGRESS 여부를 다시 확인해 한쪽만 반영되게 한다.
        InterviewSession current = interviewSessionRepository.findByIdForUpdate(session.getId()).orElse(null);
        if (current == null || current.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new InterviewException(InterviewErrorCode.SESSION_ALREADY_ENDED);
        }

        current.markAbandoned(cause);
        interviewSessionRepository.save(current);

        // 이용권 처리 실패 시 트랜잭션 전체를 롤백한다(try/catch로 삼키지 않음) — InterviewSttResetPersister와 동일 원칙.
        if (cause == AbandonCause.USER_EXIT) {
            // TODO: 이용권 커밋을 여기서 바로 하지 않는다 — 나중에 보고서 생성 성공 시 이용권 커밋, 실패 시 이용권 환불하는 방향으로 미룰 예정.
            triggerReportGeneration(current.getId());
            return new PersistResult(current.getEndedAt(), "COMMITTED", true);
        }

        ticketReleaseUseCase.release(current.getId(), cause.name());
        return new PersistResult(current.getEndedAt(), "RELEASED", false);
    }

    private void triggerReportGeneration(Long sessionId) {
        try {
            interviewReportGenerateUseCase.generate(sessionId);
        } catch (RejectedExecutionException e) {
            interviewReportFailureHandler.markFailed(sessionId);
        }
    }
}

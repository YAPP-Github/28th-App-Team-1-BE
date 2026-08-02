package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
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

        // 이용권은 여기서 커밋하지 않는다 — 리포트 생성 트리거만 하고, 실제 커밋/환급은 그 결과에 따라
        // InterviewReportGenerateService(성공 시 커밋)와 InterviewReportFailureHandler(실패 시 환급)가 처리한다.
        if (cause == AbandonCause.USER_EXIT) {
            triggerReportGeneration(current.getId());
            return new PersistResult(current.getEndedAt(), "HELD", true);
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

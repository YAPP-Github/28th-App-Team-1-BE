package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionResumeConfirmUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeConfirmResult;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.ticket.application.port.in.TicketReleaseUseCase;
import com.yapp.d14.ticket.application.port.in.TicketReservationStatusQueryUseCase;
import com.yapp.d14.ticket.application.port.in.result.TicketReservationHoldStatusResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InterviewSessionResumeConfirmService implements InterviewSessionResumeConfirmUseCase {

    // InterviewSessionResumeQueryService(API A)와 같은 값 — hold TTL/재개 가능 상한 통일.
    private static final Duration HOLD_TTL = Duration.ofMinutes(20);

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final TicketReservationStatusQueryUseCase ticketReservationStatusQueryUseCase;
    private final TicketReleaseUseCase ticketReleaseUseCase;

    @Override
    @Transactional
    public InterviewSessionResumeConfirmResult confirmResume(UUID userId, Long sessionId) {
        InterviewSession session = InterviewSessionAccessSupport.requireOwned(interviewSessionRepository, sessionId, userId);

        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new InterviewException(InterviewErrorCode.SESSION_ALREADY_ENDED);
        }

        TicketReservationHoldStatusResult hold = ticketReservationStatusQueryUseCase.getHoldStatus(sessionId);
        if (!hold.held() || !withinTtl(hold.heldAt())) {
            // API A 조회와 이 호출 사이에 다른 경로(다른 기기의 checkAvailable 등)가 끼어든 레이스 — 409가 아니라 200 sessionEnded로 응답.
            // 여기서는 재개를 확정하려던 시점이라 뒤로 미루지 않고 즉시 환불한다(releaseIfHeld는 원자적이라 이미 COMMIT된 경우엔 안전하게 무시됨).
            session.markAbandoned(AbandonCause.HOLD_EXPIRED);
            interviewSessionRepository.save(session);
            ticketReleaseUseCase.release(sessionId, AbandonCause.HOLD_EXPIRED.name());
            return InterviewSessionResumeConfirmResult.holdExpired(
                    InterviewSessionStatus.ABANDONED.name(), session.getAbandonCause(), session.getEndedAt()
            );
        }

        Question latest = questionRepository.findLatestBySessionId(sessionId)
                .orElseThrow(() -> new IllegalStateException("진행중인 세션에 질문이 없어요. sessionId=" + sessionId));
        InterviewAnswerSubmitResult.NextQuestion nextQuestion = new InterviewAnswerSubmitResult.NextQuestion(
                latest.getId(), Boolean.TRUE.equals(latest.getIsWrapUp()), latest.getTurnLevel(), latest.getDepthLevel()
        );
        return InterviewSessionResumeConfirmResult.nextQuestion(nextQuestion);
    }

    private boolean withinTtl(LocalDateTime heldAt) {
        return heldAt != null && heldAt.plus(HOLD_TTL).isAfter(LocalDateTime.now());
    }
}

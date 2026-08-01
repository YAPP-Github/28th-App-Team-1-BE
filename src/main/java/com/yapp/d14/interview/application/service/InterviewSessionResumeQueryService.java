package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewSessionResumeQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeResult;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
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
class InterviewSessionResumeQueryService implements InterviewSessionResumeQueryUseCase {

    // TicketAvailabilityCheckService.HOLD_TTL과 같은 값(임시 20분) — 재개 가능 상한이자 이용권 정산 기준으로 통일.
    private static final Duration HOLD_TTL = Duration.ofMinutes(20);

    private final InterviewSessionRepository interviewSessionRepository;
    private final TicketReservationStatusQueryUseCase ticketReservationStatusQueryUseCase;

    @Override
    @Transactional
    public InterviewSessionResumeResult resume(UUID userId, Long sessionId) {
        InterviewSession session = InterviewSessionAccessSupport.requireOwned(interviewSessionRepository, sessionId, userId);

        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            return InterviewSessionResumeResult.ended(session.getStatus().name());
        }

        TicketReservationHoldStatusResult hold = ticketReservationStatusQueryUseCase.getHoldStatus(sessionId);
        if (hold.held() && withinTtl(hold.heldAt())) {
            long elapsedSeconds = Duration.between(session.getStartedAt(), LocalDateTime.now()).getSeconds();
            return InterviewSessionResumeResult.resumable(session.getStartedAt(), elapsedSeconds);
        }

        // TODO: 보고서 생성이면 이용권 차감(COMMIT), 보고서 미생성이면 이용권 환불(RELEASE)하는 방향으로 구현 예정 — 아직 미확정.
        session.markAbandoned(AbandonCause.HOLD_EXPIRED);
        interviewSessionRepository.save(session);
        return InterviewSessionResumeResult.ended(InterviewSessionStatus.ABANDONED.name());
    }

    private boolean withinTtl(LocalDateTime heldAt) {
        return heldAt != null && heldAt.plus(HOLD_TTL).isAfter(LocalDateTime.now());
    }
}

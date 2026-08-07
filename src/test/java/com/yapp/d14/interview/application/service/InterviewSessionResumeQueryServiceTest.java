package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeResult;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.ticket.application.port.in.TicketReleaseUseCase;
import com.yapp.d14.ticket.application.port.in.TicketReservationStatusQueryUseCase;
import com.yapp.d14.ticket.application.port.in.result.TicketReservationHoldStatusResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewSessionResumeQueryServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private TicketReservationStatusQueryUseCase ticketReservationStatusQueryUseCase;

    @Mock
    private TicketReleaseUseCase ticketReleaseUseCase;

    @InjectMocks
    private InterviewSessionResumeQueryService service;

    private final UUID userId = UUID.randomUUID();
    private final Long sessionId = 1L;

    private InterviewSession session(InterviewSessionStatus status, LocalDateTime startedAt) {
        return InterviewSession.of(
                sessionId, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now(), status, startedAt, null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null, null
        );
    }

    @Test
    void 이미_종료된_세션이면_이용권_확인_없이_ENDED를_반환한다() {
        given(interviewSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session(InterviewSessionStatus.COMPLETED, LocalDateTime.now())));

        InterviewSessionResumeResult result = service.resume(userId, sessionId);

        assertThat(result.resumeState()).isEqualTo("ENDED");
        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(ticketReservationStatusQueryUseCase, never()).getHoldStatus(any());
        verify(interviewSessionRepository, never()).save(any());
    }

    @Test
    void 진행중이고_예약이_HELD고_TTL_이내면_RESUMABLE을_반환한다() {
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(5);
        given(interviewSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session(InterviewSessionStatus.IN_PROGRESS, startedAt)));
        given(ticketReservationStatusQueryUseCase.getHoldStatus(sessionId))
                .willReturn(new TicketReservationHoldStatusResult(true, LocalDateTime.now().minusMinutes(5)));

        InterviewSessionResumeResult result = service.resume(userId, sessionId);

        assertThat(result.resumeState()).isEqualTo("RESUMABLE");
        assertThat(result.startedAt()).isEqualTo(startedAt);
        assertThat(result.elapsedSeconds()).isGreaterThanOrEqualTo(0);
        verify(interviewSessionRepository, never()).save(any());
        verify(ticketReleaseUseCase, never()).release(any(), any());
    }

    @Test
    void 진행중인데_예약이_HELD가_아니면_HOLD_EXPIRED로_read_repair하고_ABANDONED를_반환한다() {
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(5));
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(ticketReservationStatusQueryUseCase.getHoldStatus(sessionId))
                .willReturn(new TicketReservationHoldStatusResult(false, null));

        InterviewSessionResumeResult result = service.resume(userId, sessionId);

        assertThat(result.resumeState()).isEqualTo("ENDED");
        assertThat(result.status()).isEqualTo("ABANDONED");
        assertThat(session.getAbandonCause()).isEqualTo(AbandonCause.HOLD_EXPIRED);
        verify(interviewSessionRepository).save(session);
        verify(ticketReleaseUseCase).release(sessionId, AbandonCause.HOLD_EXPIRED.name());
    }

    @Test
    void 진행중이고_예약은_HELD인데_heldAt이_TTL을_초과했으면_HOLD_EXPIRED로_정리하고_이용권을_환불한다() {
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now().minusMinutes(30));
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(ticketReservationStatusQueryUseCase.getHoldStatus(sessionId))
                .willReturn(new TicketReservationHoldStatusResult(true, LocalDateTime.now().minusMinutes(25)));

        InterviewSessionResumeResult result = service.resume(userId, sessionId);

        assertThat(result.resumeState()).isEqualTo("ENDED");
        assertThat(result.status()).isEqualTo("ABANDONED");
        verify(interviewSessionRepository).save(session);
        verify(ticketReleaseUseCase).release(sessionId, AbandonCause.HOLD_EXPIRED.name());
    }

    @Test
    void 남의_세션이면_404_예외를_던진다() {
        given(interviewSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session(InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now())));

        assertThatThrownBy(() -> service.resume(UUID.randomUUID(), sessionId))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND);
    }
}

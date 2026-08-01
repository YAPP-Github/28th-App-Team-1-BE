package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeConfirmResult;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
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
class InterviewSessionResumeConfirmServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private TicketReservationStatusQueryUseCase ticketReservationStatusQueryUseCase;

    @Mock
    private TicketReleaseUseCase ticketReleaseUseCase;

    @InjectMocks
    private InterviewSessionResumeConfirmService service;

    private final UUID userId = UUID.randomUUID();
    private final Long sessionId = 1L;

    private InterviewSession session(InterviewSessionStatus status) {
        return InterviewSession.of(
                sessionId, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now(), status, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null
        );
    }

    @Test
    void 이미_종료된_세션이면_409_SESSION_ALREADY_ENDED를_던진다() {
        given(interviewSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session(InterviewSessionStatus.COMPLETED)));

        assertThatThrownBy(() -> service.confirmResume(userId, sessionId))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.SESSION_ALREADY_ENDED);
        verify(ticketReservationStatusQueryUseCase, never()).getHoldStatus(any());
    }

    @Test
    void 진행중이고_hold가_유효하면_최신_턴_질문을_nextQuestion으로_반환한다() {
        given(interviewSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session(InterviewSessionStatus.IN_PROGRESS)));
        given(ticketReservationStatusQueryUseCase.getHoldStatus(sessionId))
                .willReturn(new TicketReservationHoldStatusResult(true, LocalDateTime.now().minusMinutes(5)));
        Question latest = Question.of(
                99L, sessionId, "질문 내용", 3, 1, null, null, null, null, null, false, LocalDateTime.now()
        );
        given(questionRepository.findLatestBySessionId(sessionId)).willReturn(Optional.of(latest));

        InterviewSessionResumeConfirmResult result = service.confirmResume(userId, sessionId);

        assertThat(result.sessionEnded()).isFalse();
        assertThat(result.nextQuestion().questionId()).isEqualTo(99L);
        assertThat(result.nextQuestion().turnLevel()).isEqualTo(3);
        assertThat(result.nextQuestion().depthLevel()).isEqualTo(1);
        assertThat(result.nextQuestion().isLast()).isFalse();
        verify(interviewSessionRepository, never()).save(any());
        verify(ticketReleaseUseCase, never()).release(any(), any());
    }

    @Test
    void hold가_풀려있으면_레이스로_간주해_ABANDONED_전환_후_이용권을_환불하고_sessionEnded를_반환한다() {
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS);
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(ticketReservationStatusQueryUseCase.getHoldStatus(sessionId))
                .willReturn(new TicketReservationHoldStatusResult(false, null));

        InterviewSessionResumeConfirmResult result = service.confirmResume(userId, sessionId);

        assertThat(result.sessionEnded()).isTrue();
        assertThat(result.status()).isEqualTo("ABANDONED");
        assertThat(result.abandonCause()).isEqualTo(AbandonCause.HOLD_EXPIRED);
        assertThat(session.getAbandonCause()).isEqualTo(AbandonCause.HOLD_EXPIRED);
        verify(interviewSessionRepository).save(session);
        verify(ticketReleaseUseCase).release(sessionId, AbandonCause.HOLD_EXPIRED.name());
    }

    @Test
    void hold는_있지만_TTL을_초과했으면_같은_방식으로_정리한다() {
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS);
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(ticketReservationStatusQueryUseCase.getHoldStatus(sessionId))
                .willReturn(new TicketReservationHoldStatusResult(true, LocalDateTime.now().minusMinutes(25)));

        InterviewSessionResumeConfirmResult result = service.confirmResume(userId, sessionId);

        assertThat(result.sessionEnded()).isTrue();
        assertThat(result.status()).isEqualTo("ABANDONED");
        verify(ticketReleaseUseCase).release(sessionId, AbandonCause.HOLD_EXPIRED.name());
    }

    @Test
    void 남의_세션이면_404_예외를_던진다() {
        given(interviewSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session(InterviewSessionStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> service.confirmResume(UUID.randomUUID(), sessionId))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND);
    }
}

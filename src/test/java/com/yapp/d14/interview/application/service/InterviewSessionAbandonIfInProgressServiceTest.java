package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewSessionAbandonIfInProgressServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @InjectMocks
    private InterviewSessionAbandonIfInProgressService service;

    private final Long sessionId = 1L;

    private InterviewSession session(InterviewSessionStatus status) {
        return InterviewSession.of(
                sessionId, UUID.randomUUID(), UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now(), status, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null, null
        );
    }

    @Test
    void IN_PROGRESS_세션이면_전달받은_cause로_ABANDONED로_전환하고_저장한_뒤_true를_반환한다() {
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS);
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(session));

        boolean abandoned = service.abandon(sessionId, AbandonCause.HOLD_EXPIRED);

        assertThat(abandoned).isTrue();
        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.ABANDONED);
        assertThat(session.getAbandonCause()).isEqualTo(AbandonCause.HOLD_EXPIRED);
        verify(interviewSessionRepository).save(session);
    }

    @Test
    void SESSION_SUPERSEDED_cause로도_전환된다() {
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS);
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(session));

        boolean abandoned = service.abandon(sessionId, AbandonCause.SESSION_SUPERSEDED);

        assertThat(abandoned).isTrue();
        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.ABANDONED);
        assertThat(session.getAbandonCause()).isEqualTo(AbandonCause.SESSION_SUPERSEDED);
        verify(interviewSessionRepository).save(session);
    }

    @Test
    void 이미_종료된_세션이면_아무것도_하지_않고_false를_반환한다() {
        InterviewSession session = session(InterviewSessionStatus.COMPLETED);
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(session));

        boolean abandoned = service.abandon(sessionId, AbandonCause.HOLD_EXPIRED);

        assertThat(abandoned).isFalse();
        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.COMPLETED);
        verify(interviewSessionRepository, never()).save(session);
    }

    @Test
    void 세션이_없으면_아무것도_하지_않고_false를_반환한다() {
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.empty());

        boolean abandoned = service.abandon(sessionId, AbandonCause.HOLD_EXPIRED);

        assertThat(abandoned).isFalse();
        verify(interviewSessionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

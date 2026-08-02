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
class InterviewSessionAbandonOnHoldExpiryServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @InjectMocks
    private InterviewSessionAbandonOnHoldExpiryService service;

    private final Long sessionId = 1L;

    private InterviewSession session(InterviewSessionStatus status) {
        return InterviewSession.of(
                sessionId, UUID.randomUUID(), UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now(), status, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null
        );
    }

    @Test
    void IN_PROGRESS_세션이면_ABANDONED_HOLD_EXPIRED로_전환하고_저장한다() {
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS);
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        service.abandonForHoldExpiry(sessionId);

        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.ABANDONED);
        assertThat(session.getAbandonCause()).isEqualTo(AbandonCause.HOLD_EXPIRED);
        verify(interviewSessionRepository).save(session);
    }

    @Test
    void 이미_종료된_세션이면_아무것도_하지_않는다() {
        InterviewSession session = session(InterviewSessionStatus.COMPLETED);
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        service.abandonForHoldExpiry(sessionId);

        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.COMPLETED);
        verify(interviewSessionRepository, never()).save(session);
    }

    @Test
    void 세션이_없으면_아무것도_하지_않는다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        service.abandonForHoldExpiry(sessionId);

        verify(interviewSessionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

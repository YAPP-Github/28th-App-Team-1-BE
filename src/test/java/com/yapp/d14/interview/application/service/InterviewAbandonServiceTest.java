package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewAbandonCommand;
import com.yapp.d14.interview.application.port.in.result.InterviewAbandonResult;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
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
class InterviewAbandonServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private InterviewAbandonPersister interviewAbandonPersister;

    @InjectMocks
    private InterviewAbandonService service;

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
    void 진행중인_세션이면_persister에_위임하고_결과를_조립한다() {
        InterviewSession session = session(InterviewSessionStatus.IN_PROGRESS);
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        LocalDateTime endedAt = LocalDateTime.now();
        given(interviewAbandonPersister.persist(session, AbandonCause.USER_EXIT))
                .willReturn(new InterviewAbandonPersister.PersistResult(endedAt, "HELD", true));

        InterviewAbandonResult result = service.abandon(userId, new InterviewAbandonCommand(sessionId, AbandonCause.USER_EXIT));

        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.status()).isEqualTo("ABANDONED");
        assertThat(result.abandonCause()).isEqualTo(AbandonCause.USER_EXIT);
        assertThat(result.endedAt()).isEqualTo(endedAt);
        assertThat(result.ticketOutcome()).isEqualTo("HELD");
        assertThat(result.reportGenerating()).isTrue();
    }

    @Test
    void 이미_종료된_세션이면_409_SESSION_ALREADY_ENDED를_던지고_persister를_호출하지_않는다() {
        given(interviewSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session(InterviewSessionStatus.COMPLETED)));

        assertThatThrownBy(() -> service.abandon(userId, new InterviewAbandonCommand(sessionId, AbandonCause.USER_EXIT)))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.SESSION_ALREADY_ENDED);
        verify(interviewAbandonPersister, never()).persist(any(), any());
    }

    @Test
    void 남의_세션이면_404_예외를_던진다() {
        given(interviewSessionRepository.findById(sessionId))
                .willReturn(Optional.of(session(InterviewSessionStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> service.abandon(UUID.randomUUID(), new InterviewAbandonCommand(sessionId, AbandonCause.USER_EXIT)))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND);
    }
}

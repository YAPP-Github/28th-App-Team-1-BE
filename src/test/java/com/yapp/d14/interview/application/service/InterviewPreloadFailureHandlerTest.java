package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.ticket.application.port.in.TicketReleaseUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewPreloadFailureHandlerTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private QuestionCandidateRepository questionCandidateRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private TicketReleaseUseCase ticketReleaseUseCase;

    @InjectMocks
    private InterviewPreloadFailureHandler handler;

    private final UUID userId = UUID.randomUUID();

    private InterviewSession preparingSession() {
        return InterviewSession.of(
                1L, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null, LocalDateTime.now(),
                InterviewSessionStatus.PREPARING, null, null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null, null
        );
    }

    @Test
    void 세션을_preload_failed로_전환하고_이용권을_release한다() {
        given(interviewSessionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(preparingSession()));
        given(interviewSessionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        boolean applied = handler.markFailed(1L);

        assertThat(applied).isTrue();
        ArgumentCaptor<InterviewSession> captor = ArgumentCaptor.forClass(InterviewSession.class);
        verify(interviewSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterviewSessionStatus.PRELOAD_FAILED);
        verify(questionCandidateRepository).deleteBySessionId(1L);
        verify(questionRepository).deleteBySessionId(1L);
        verify(ticketReleaseUseCase).release(1L, "PRELOAD_FAILED");
    }

    @Test
    void 세션을_찾을_수_없으면_아무것도_하지_않는다() {
        given(interviewSessionRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        boolean applied = handler.markFailed(1L);

        assertThat(applied).isFalse();
        verify(interviewSessionRepository, never()).save(any());
        verify(questionCandidateRepository, never()).deleteBySessionId(any());
        verify(questionRepository, never()).deleteBySessionId(any());
        verify(ticketReleaseUseCase, never()).release(any(), any());
    }

    @Test
    void release가_실패해도_세션은_preload_failed로_유지된다() {
        given(interviewSessionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(preparingSession()));
        given(interviewSessionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        willThrow(new RuntimeException("release 실패")).given(ticketReleaseUseCase).release(1L, "PRELOAD_FAILED");

        boolean applied = handler.markFailed(1L);

        assertThat(applied).isTrue();
        ArgumentCaptor<InterviewSession> captor = ArgumentCaptor.forClass(InterviewSession.class);
        verify(interviewSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterviewSessionStatus.PRELOAD_FAILED);
    }

    @Test
    void 늦은_preload_성공으로_이미_READY로_전환된_세션이면_실패_처리를_건너뛴다() {
        InterviewSession readySession = InterviewSession.of(
                1L, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null, LocalDateTime.now(),
                InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null, null
        );
        given(interviewSessionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(readySession));

        boolean applied = handler.markFailed(1L);

        assertThat(applied).isFalse();
        verify(interviewSessionRepository, never()).save(any());
        verify(questionCandidateRepository, never()).deleteBySessionId(any());
        verify(questionRepository, never()).deleteBySessionId(any());
        verify(ticketReleaseUseCase, never()).release(any(), any());
    }
}

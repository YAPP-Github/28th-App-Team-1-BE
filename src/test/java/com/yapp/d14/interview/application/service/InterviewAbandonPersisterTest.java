package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import com.yapp.d14.ticket.application.port.in.TicketReleaseUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewAbandonPersisterTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private TicketReleaseUseCase ticketReleaseUseCase;

    @Mock
    private InterviewReportGenerateUseCase interviewReportGenerateUseCase;

    @Mock
    private InterviewReportFailureHandler interviewReportFailureHandler;

    @Mock
    private AccountReviewThresholdReporter accountReviewThresholdReporter;

    @InjectMocks
    private InterviewAbandonPersister persister;

    private final Long sessionId = 1L;

    private InterviewSession session() {
        return InterviewSession.of(
                sessionId, UUID.randomUUID(), UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now(), InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null
        );
    }

    @Test
    void USER_EXIT면_이용권_보류를_유지한_채_리포트_생성만_트리거한다() {
        // 이용권 커밋/환급은 리포트 생성 결과에 따라 InterviewReportGenerateService/InterviewReportFailureHandler가 처리한다.
        InterviewSession session = session();
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(session));

        InterviewAbandonPersister.PersistResult result = persister.persist(session, AbandonCause.USER_EXIT);

        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.ABANDONED);
        assertThat(session.getAbandonCause()).isEqualTo(AbandonCause.USER_EXIT);
        assertThat(result.ticketOutcome()).isEqualTo("HELD");
        assertThat(result.reportGenerating()).isTrue();
        verify(interviewSessionRepository).save(session);
        verify(ticketReleaseUseCase, never()).release(any(), any());
        verify(interviewReportGenerateUseCase).generate(sessionId);
    }

    @Test
    void NETWORK_DISCONNECT면_이용권을_환급하고_리포트를_생성하지_않는다() {
        InterviewSession session = session();
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(session));

        InterviewAbandonPersister.PersistResult result = persister.persist(session, AbandonCause.NETWORK_DISCONNECT);

        assertThat(session.getAbandonCause()).isEqualTo(AbandonCause.NETWORK_DISCONNECT);
        assertThat(result.ticketOutcome()).isEqualTo("RELEASED");
        assertThat(result.reportGenerating()).isFalse();
        verify(ticketReleaseUseCase).release(sessionId, "NETWORK_DISCONNECT");
        verify(interviewReportGenerateUseCase, never()).generate(any());
        verify(accountReviewThresholdReporter).reportIfThresholdReached(session.getUserId());
    }

    @Test
    void USER_EXIT는_운영_검토_집계_대상이_아니다() {
        InterviewSession session = session();
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(session));

        persister.persist(session, AbandonCause.USER_EXIT);

        verify(accountReviewThresholdReporter, never()).reportIfThresholdReached(any());
    }

    @Test
    void 리포트_생성_큐가_가득_찼으면_실패_처리로_넘어가고_예외를_삼킨다() {
        InterviewSession session = session();
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(session));
        org.mockito.Mockito.doThrow(new RejectedExecutionException())
                .when(interviewReportGenerateUseCase).generate(sessionId);

        persister.persist(session, AbandonCause.USER_EXIT);

        verify(interviewReportFailureHandler).markFailed(sessionId);
    }

    @Test
    void 락_획득_시점에_이미_IN_PROGRESS가_아니면_SESSION_ALREADY_ENDED로_실패하고_아무것도_처리하지_않는다() {
        // 서비스 레이어의 상태 확인 이후, 이 저장 사이에 다른 요청이 먼저 세션을 종료시킨 경합 상황을 흉내낸다.
        InterviewSession alreadyAbandoned = InterviewSession.of(
                sessionId, UUID.randomUUID(), UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now(), InterviewSessionStatus.ABANDONED, LocalDateTime.now(), LocalDateTime.now(), null,
                25, 20, 10, 20, 10, 15, 0, 0, AbandonCause.NETWORK_DISCONNECT
        );
        given(interviewSessionRepository.findByIdForUpdate(sessionId)).willReturn(Optional.of(alreadyAbandoned));

        assertThatThrownBy(() -> persister.persist(session(), AbandonCause.USER_EXIT))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.SESSION_ALREADY_ENDED);

        verify(interviewSessionRepository, never()).save(any());
        verify(ticketReleaseUseCase, never()).release(any(), any());
        verify(interviewReportGenerateUseCase, never()).generate(any());
    }
}

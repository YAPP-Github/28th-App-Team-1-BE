package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionFileCleaner;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InterviewSessionFileCleanupServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private InterviewSessionFileCleaner interviewSessionFileCleaner;

    @InjectMocks
    private InterviewSessionFileCleanupService interviewSessionFileCleanupService;

    @Test
    void 대상_세션의_S3_잔여물을_지우고_정리_시각을_남긴다() {
        InterviewSession session = abandonedSession(42L, AbandonCause.NETWORK_DISCONNECT);
        given(interviewSessionRepository.findFileCleanupTargets(any(), anyInt())).willReturn(List.of(session));

        int cleaned = interviewSessionFileCleanupService.cleanupOrphanFiles();

        assertThat(cleaned).isEqualTo(1);
        verify(interviewSessionFileCleaner).deleteSessionFiles(session.getUserId(), 42L);

        ArgumentCaptor<InterviewSession> captor = ArgumentCaptor.forClass(InterviewSession.class);
        verify(interviewSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getFilesCleanedAt()).isNotNull();
    }

    @Test
    void 종료_후_유예_시간이_지난_세션만_조회한다() {
        given(interviewSessionRepository.findFileCleanupTargets(any(), anyInt())).willReturn(List.of());

        interviewSessionFileCleanupService.cleanupOrphanFiles();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(interviewSessionRepository).findFileCleanupTargets(captor.capture(), anyInt());
        assertThat(captor.getValue()).isBefore(LocalDateTime.now().minusMinutes(50));
    }

    @Test
    void 대상이_없으면_S3를_건드리지_않는다() {
        given(interviewSessionRepository.findFileCleanupTargets(any(), anyInt())).willReturn(List.of());

        int cleaned = interviewSessionFileCleanupService.cleanupOrphanFiles();

        assertThat(cleaned).isZero();
        verifyNoInteractions(interviewSessionFileCleaner);
    }

    @Test
    void S3_삭제에_실패하면_정리_시각을_남기지_않아_다음_실행에서_다시_시도한다() {
        InterviewSession session = abandonedSession(42L, AbandonCause.HOLD_EXPIRED);
        given(interviewSessionRepository.findFileCleanupTargets(any(), anyInt())).willReturn(List.of(session));
        willThrow(new RuntimeException("s3 down"))
                .given(interviewSessionFileCleaner).deleteSessionFiles(any(), eq(42L));

        int cleaned = interviewSessionFileCleanupService.cleanupOrphanFiles();

        assertThat(cleaned).isZero();
        assertThat(session.getFilesCleanedAt()).isNull();
        verify(interviewSessionRepository, never()).save(any());
    }

    @Test
    void 한_세션이_실패해도_나머지는_계속_정리한다() {
        InterviewSession failing = abandonedSession(1L, AbandonCause.HOLD_EXPIRED);
        InterviewSession succeeding = abandonedSession(2L, AbandonCause.NETWORK_DISCONNECT);
        given(interviewSessionRepository.findFileCleanupTargets(any(), anyInt()))
                .willReturn(List.of(failing, succeeding));
        willThrow(new RuntimeException("s3 down"))
                .given(interviewSessionFileCleaner).deleteSessionFiles(any(), eq(1L));

        int cleaned = interviewSessionFileCleanupService.cleanupOrphanFiles();

        assertThat(cleaned).isEqualTo(1);
        verify(interviewSessionFileCleaner).deleteSessionFiles(succeeding.getUserId(), 2L);
        assertThat(succeeding.getFilesCleanedAt()).isNotNull();
    }

    private InterviewSession abandonedSession(Long sessionId, AbandonCause cause) {
        return InterviewSession.of(
                sessionId, UUID.randomUUID(), UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                LocalDateTime.now().minusHours(3), InterviewSessionStatus.ABANDONED,
                LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2), null,
                25, 20, 10, 20, 10, 15, 0, 0, cause, null
        );
    }
}

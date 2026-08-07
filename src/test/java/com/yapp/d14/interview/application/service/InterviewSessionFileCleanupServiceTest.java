package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.FileCleanupTarget;
import com.yapp.d14.interview.application.port.out.InterviewSessionFileCleaner;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
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
    void 리포트를_만들지_않는_상태와_유예_시간을_조회_조건으로_넘긴다() {
        given(interviewSessionRepository.findFileCleanupTargets(any(), any(), any(), anyInt())).willReturn(List.of());

        interviewSessionFileCleanupService.cleanupOrphanFiles();

        ArgumentCaptor<List<InterviewSessionStatus>> statuses = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDateTime> endedBefore = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(interviewSessionRepository).findFileCleanupTargets(
                statuses.capture(), eq(AbandonCause.USER_EXIT), endedBefore.capture(), anyInt()
        );
        assertThat(statuses.getValue())
                .containsExactlyInAnyOrder(InterviewSessionStatus.INVALID, InterviewSessionStatus.PRELOAD_FAILED);
        assertThat(endedBefore.getValue()).isBefore(LocalDateTime.now().minusMinutes(50));
    }

    @Test
    void 대상_세션의_S3_잔여물을_지우고_정리_시각을_한번에_기록한다() {
        FileCleanupTarget target = target(42L);
        given(interviewSessionRepository.findFileCleanupTargets(any(), any(), any(), anyInt()))
                .willReturn(List.of(target));

        int cleaned = interviewSessionFileCleanupService.cleanupOrphanFiles();

        assertThat(cleaned).isEqualTo(1);
        verify(interviewSessionFileCleaner).deleteSessionFiles(target.userId(), 42L);
        verify(interviewSessionRepository).markFilesCleaned(eq(List.of(42L)), any());
    }

    @Test
    void 대상이_없으면_S3도_기록도_건드리지_않는다() {
        given(interviewSessionRepository.findFileCleanupTargets(any(), any(), any(), anyInt())).willReturn(List.of());

        int cleaned = interviewSessionFileCleanupService.cleanupOrphanFiles();

        assertThat(cleaned).isZero();
        verifyNoInteractions(interviewSessionFileCleaner);
        verify(interviewSessionRepository, never()).markFilesCleaned(any(), any());
    }

    @Test
    void S3_삭제에_실패하면_정리_시각을_남기지_않아_다음_실행에서_다시_시도한다() {
        given(interviewSessionRepository.findFileCleanupTargets(any(), any(), any(), anyInt()))
                .willReturn(List.of(target(42L)));
        willThrow(new RuntimeException("s3 down"))
                .given(interviewSessionFileCleaner).deleteSessionFiles(any(), eq(42L));

        int cleaned = interviewSessionFileCleanupService.cleanupOrphanFiles();

        assertThat(cleaned).isZero();
        verify(interviewSessionRepository, never()).markFilesCleaned(any(), any());
    }

    @Test
    void 한_세션이_실패해도_나머지는_계속_정리하고_성공한_것만_기록한다() {
        FileCleanupTarget failing = target(1L);
        FileCleanupTarget succeeding = target(2L);
        given(interviewSessionRepository.findFileCleanupTargets(any(), any(), any(), anyInt()))
                .willReturn(List.of(failing, succeeding));
        willThrow(new RuntimeException("s3 down"))
                .given(interviewSessionFileCleaner).deleteSessionFiles(any(), eq(1L));

        int cleaned = interviewSessionFileCleanupService.cleanupOrphanFiles();

        assertThat(cleaned).isEqualTo(1);
        verify(interviewSessionFileCleaner).deleteSessionFiles(succeeding.userId(), 2L);
        verify(interviewSessionRepository).markFilesCleaned(eq(List.of(2L)), any());
    }

    private FileCleanupTarget target(Long sessionId) {
        return new FileCleanupTarget(sessionId, UUID.randomUUID(), InterviewSessionStatus.ABANDONED);
    }
}

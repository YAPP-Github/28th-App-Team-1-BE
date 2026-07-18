package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionPollStatus;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
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
class InterviewSessionStatusQueryServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewVoiceStorage interviewVoiceStorage;

    @InjectMocks
    private InterviewSessionStatusQueryService service;

    private final UUID userId = UUID.randomUUID();

    private InterviewSession sessionWithStatus(InterviewSessionStatus status, LocalDateTime startedAt) {
        return InterviewSession.of(
                1L, userId, UUID.randomUUID(), JobType.BACKEND, 3, null, null, null,
                status, startedAt, null, null,
                25, 20, 10, 20, 10, 15
        );
    }

    @Test
    void PREPARING이면_PROCESSING을_반환한다() {
        given(interviewSessionRepository.findById(1L))
                .willReturn(Optional.of(sessionWithStatus(InterviewSessionStatus.PREPARING, null)));

        InterviewSessionStatusResult result = service.getStatus(userId, 1L);

        assertThat(result.status()).isEqualTo(InterviewSessionPollStatus.PROCESSING);
        assertThat(result.summaryQuestion()).isNull();
    }

    @Test
    void PRELOAD_FAILED면_FAILED를_반환한다() {
        given(interviewSessionRepository.findById(1L))
                .willReturn(Optional.of(sessionWithStatus(InterviewSessionStatus.PRELOAD_FAILED, null)));

        InterviewSessionStatusResult result = service.getStatus(userId, 1L);

        assertThat(result.status()).isEqualTo(InterviewSessionPollStatus.FAILED);
    }

    @Test
    void IN_PROGRESS면_READY와_요약질문을_반환한다() {
        LocalDateTime startedAt = LocalDateTime.now();
        given(interviewSessionRepository.findById(1L))
                .willReturn(Optional.of(sessionWithStatus(InterviewSessionStatus.IN_PROGRESS, startedAt)));
        Question question = Question.of(
                10L, 1L, "요약 질문", 0, 0, null, null, null, null, "s3-key", false, LocalDateTime.now()
        );
        given(questionRepository.findBySessionIdAndTurnLevel(1L, 0)).willReturn(Optional.of(question));
        given(interviewVoiceStorage.readBase64("s3-key")).willReturn("base64-audio");

        InterviewSessionStatusResult result = service.getStatus(userId, 1L);

        assertThat(result.status()).isEqualTo(InterviewSessionPollStatus.READY);
        assertThat(result.startedAt()).isEqualTo(startedAt);
        assertThat(result.summaryQuestion().questionId()).isEqualTo(10L);
        assertThat(result.summaryQuestion().ttsAudio()).isEqualTo("base64-audio");
        assertThat(result.summaryQuestion().turnLevel()).isZero();
        assertThat(result.summaryQuestion().depthLevel()).isZero();
    }

    @Test
    void 요약질문에_음성이_없으면_ttsAudio는_null이고_S3를_조회하지_않는다() {
        given(interviewSessionRepository.findById(1L))
                .willReturn(Optional.of(sessionWithStatus(InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now())));
        Question question = Question.of(
                10L, 1L, "요약 질문", 0, 0, null, null, null, null, null, false, LocalDateTime.now()
        );
        given(questionRepository.findBySessionIdAndTurnLevel(1L, 0)).willReturn(Optional.of(question));

        InterviewSessionStatusResult result = service.getStatus(userId, 1L);

        assertThat(result.summaryQuestion().ttsAudio()).isNull();
        verify(interviewVoiceStorage, never()).readBase64(any());
    }

    @Test
    void 세션이_없거나_본인_소유가_아니면_404() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatus(userId, 1L))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND);
    }

    @Test
    void 세션이_존재하지만_다른_사용자_소유면_404() {
        UUID otherUserId = UUID.randomUUID();
        given(interviewSessionRepository.findById(1L))
                .willReturn(Optional.of(sessionWithStatus(InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now())));

        assertThatThrownBy(() -> service.getStatus(otherUserId, 1L))
                .isInstanceOf(InterviewException.class)
                .extracting(e -> ((InterviewException) e).getErrorCode())
                .isEqualTo(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND);
    }
}

package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor.AudioTrack;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.TestType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewVideoCompositeServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Long SESSION_ID = 100L;

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private InterviewVideoCompositor interviewVideoCompositor;
    @Mock
    private InterviewVideoRepository interviewVideoRepository;

    @InjectMocks
    private InterviewVideoCompositeService service;

    // aiVoiceS3Key는 요약 질문만 저장돼 있어 합성/세그먼트가 의존하지 않는다 — 키는 turnLevel로 결정적 계산되므로 null로 둔다.
    private Question question(long id, int turnLevel, Float startSec) {
        return Question.of(id, SESSION_ID, "질문", turnLevel, 1, TestType.DEPTH, "principle",
                startSec, startSec == null ? null : startSec + 1, null, false, LocalDateTime.now());
    }

    private Answer answer(long questionId, Float startSec, boolean skipped) {
        return Answer.create(SESSION_ID, questionId, "답변", startSec,
                startSec == null ? null : startSec + 3, 3f, skipped, null, null, null, null, false, false, TestType.DEPTH);
    }

    private String questionKey(int turnLevel) {
        return "users/%s/sessions/%s/questions/%s.mp3".formatted(USER_ID, SESSION_ID, turnLevel);
    }

    private String answerKey(int turnLevel) {
        return "users/%s/sessions/%s/answers/%s.webm".formatted(USER_ID, SESSION_ID, turnLevel);
    }

    @Test
    void 질문TTS와_답변음성을_모두_시작초_순으로_합성하고_완료표시한다() {
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                question(1L, 1, 5.0f),
                question(2L, 2, 12.0f)
        ));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                answer(1L, 8.0f, false),   // turnLevel 1 → answers/1.webm
                answer(2L, 15.0f, false)   // turnLevel 2 → answers/2.webm
        ));
        given(interviewVideoRepository.markComposited(SESSION_ID)).willReturn(1);

        service.composite(USER_ID, SESSION_ID);

        ArgumentCaptor<List<AudioTrack>> captor = ArgumentCaptor.forClass(List.class);
        verify(interviewVideoCompositor).compose(eq(USER_ID), eq(SESSION_ID), captor.capture());
        List<AudioTrack> tracks = captor.getValue();
        assertThat(tracks).extracting(AudioTrack::s3Key)
                .containsExactly(questionKey(1), answerKey(1), questionKey(2), answerKey(2));
        assertThat(tracks).extracting(AudioTrack::startSec).containsExactly(5.0f, 8.0f, 12.0f, 15.0f);
        verify(interviewVideoRepository).markComposited(SESSION_ID);
    }

    @Test
    void 시작초가_없는_질문과_SKIP_시작초없는_답변은_제외한다() {
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                question(1L, 1, 5.0f),
                question(2L, 2, null)   // 재생 시각 없음 → 제외
        ));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                answer(1L, 8.0f, false),
                answer(2L, 9.0f, true),   // SKIP → 제외
                answer(3L, null, false)   // 시작초 없음 → 제외
        ));

        service.composite(USER_ID, SESSION_ID);

        ArgumentCaptor<List<AudioTrack>> captor = ArgumentCaptor.forClass(List.class);
        verify(interviewVideoCompositor).compose(eq(USER_ID), eq(SESSION_ID), captor.capture());
        assertThat(captor.getValue()).extracting(AudioTrack::s3Key).containsExactly(questionKey(1), answerKey(1));
        verify(interviewVideoRepository).markComposited(SESSION_ID);
    }

    @Test
    void 합성할_오디오가_없으면_합성도_완료표시도_하지_않는다() {
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                question(1L, 1, null)   // 재생 시각 없음 → 제외
        ));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                answer(1L, null, false)
        ));

        service.composite(USER_ID, SESSION_ID);

        verify(interviewVideoCompositor, never()).compose(any(), any(), any());
        verify(interviewVideoRepository, never()).markComposited(any());
    }

    @Test
    void 합성이_실패하면_완료표시하지_않고_예외를_삼킨다() {
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                question(1L, 1, 5.0f)
        ));
        given(answerRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of());
        doThrow(new IllegalStateException("ffmpeg 실패"))
                .when(interviewVideoCompositor).compose(eq(USER_ID), eq(SESSION_ID), any());

        service.composite(USER_ID, SESSION_ID);

        verify(interviewVideoRepository, never()).markComposited(any());
    }
}

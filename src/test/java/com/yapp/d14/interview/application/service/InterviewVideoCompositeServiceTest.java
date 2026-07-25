package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor;
import com.yapp.d14.interview.application.port.out.InterviewVideoCompositor.QuestionAudioTrack;
import com.yapp.d14.interview.application.port.out.InterviewVideoRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.TestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long SESSION_ID = 100L;

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private InterviewVideoCompositor interviewVideoCompositor;
    @Mock
    private InterviewVideoRepository interviewVideoRepository;

    @InjectMocks
    private InterviewVideoCompositeService service;

    private Question question(String aiVoiceS3Key, Float startSec) {
        Question question = Question.create(
                SESSION_ID, "질문", 1, 1, TestType.DEPTH, "principle", aiVoiceS3Key, false
        );
        if (startSec != null) {
            question.markPlayed(startSec, startSec + 1);
        }
        return question;
    }

    @Test
    void 음성키와_시작초가_있는_질문만_시작초_순으로_합성하고_완료표시한다() {
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                question("q/2.mp3", 8.0f),
                question(null, 3.0f),          // 음성 없음 → 제외
                question("q/nostart.mp3", null), // 시작초 없음 → 제외
                question("q/1.mp3", 2.0f)
        ));

        service.composite(USER_ID, SESSION_ID);

        ArgumentCaptor<List<QuestionAudioTrack>> captor = ArgumentCaptor.forClass(List.class);
        verify(interviewVideoCompositor).compose(eq(USER_ID), eq(SESSION_ID), captor.capture());
        List<QuestionAudioTrack> tracks = captor.getValue();
        assertThat(tracks).extracting(QuestionAudioTrack::audioS3Key).containsExactly("q/1.mp3", "q/2.mp3");
        assertThat(tracks).extracting(QuestionAudioTrack::startSec).containsExactly(2.0f, 8.0f);
        verify(interviewVideoRepository).markComposited(SESSION_ID);
    }

    @Test
    void 합성할_질문_오디오가_없으면_합성도_완료표시도_하지_않는다() {
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                question(null, 1.0f),
                question("q/nostart.mp3", null)
        ));

        service.composite(USER_ID, SESSION_ID);

        verify(interviewVideoCompositor, never()).compose(any(), any(), any());
        verify(interviewVideoRepository, never()).markComposited(any());
    }

    @Test
    void 합성이_실패하면_완료표시하지_않고_예외를_삼킨다() {
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(
                question("q/1.mp3", 1.0f)
        ));
        doThrow(new IllegalStateException("ffmpeg 실패"))
                .when(interviewVideoCompositor).compose(eq(USER_ID), eq(SESSION_ID), any());

        service.composite(USER_ID, SESSION_ID);

        verify(interviewVideoRepository, never()).markComposited(any());
    }
}

package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.util.S3KeyGenerator;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.SpeechToTextTranscriber;
import com.yapp.d14.interview.application.port.out.TranscriptionResult;
import com.yapp.d14.interview.application.port.out.UtteranceSegmentRepository;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.ScriptRole;
import com.yapp.d14.interview.domain.TranscriptSegment;
import com.yapp.d14.interview.domain.UtteranceSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuestionUtteranceSegmentPersisterTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SpeechToTextTranscriber speechToTextTranscriber;

    @Mock
    private InterviewVoiceStorage interviewVoiceStorage;

    @Mock
    private UtteranceSegmentRepository utteranceSegmentRepository;

    @InjectMocks
    private QuestionUtteranceSegmentPersister persister;

    private static final Long SESSION_ID = 1L;
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    // 질문 음성 키는 turnLevel로 결정적 계산된다(aiVoiceS3Key에 의존하지 않음).
    private static String voiceKey(int turnLevel) {
        return S3KeyGenerator.interviewVoiceKey(USER_ID, SESSION_ID, turnLevel);
    }

    private static Question question(Long id, String content, Integer turnLevel, Float startSec) {
        return Question.of(
                id, SESSION_ID, content, turnLevel, 1, com.yapp.d14.interview.domain.TestType.DEPTH,
                null, startSec, startSec == null ? null : startSec + 5f, null, false, LocalDateTime.now()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void 질문_TTS를_결정적_키로_읽어_STT_재변환하고_questionStartSec_오프셋으로_저장한다() {
        Question question = question(10L, "안녕하세요. 반갑습니다.", 1, 12.0f);
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(question));
        given(interviewVoiceStorage.readBase64(voiceKey(1)))
                .willReturn(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}));
        given(speechToTextTranscriber.transcribe(any())).willReturn(new TranscriptionResult(
                "안녕하세요. 반갑습니다.", 2, 0,
                List.of(
                        new TranscriptSegment("안녕하세요.", 0.0f, 1.0f),
                        new TranscriptSegment(" 반갑습니다.", 1.0f, 2.0f))));

        persister.persist(USER_ID, SESSION_ID);

        // 재생성 대비로 QUESTION 세그먼트만 먼저 지운다.
        verify(utteranceSegmentRepository).deleteBySessionIdAndRole(SESSION_ID, ScriptRole.INTERVIEWER);

        ArgumentCaptor<List<UtteranceSegment>> captor = ArgumentCaptor.forClass(List.class);
        verify(utteranceSegmentRepository).saveAll(eq(SESSION_ID), eq(10L), captor.capture());
        List<UtteranceSegment> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).role()).isEqualTo(ScriptRole.INTERVIEWER);
        assertThat(saved.get(0).text()).isEqualTo("안녕하세요.");
        assertThat(saved.get(0).startSec()).isEqualTo(12.0f); // 0.0 + 12 오프셋
        assertThat(saved.get(1).text()).isEqualTo("반갑습니다.");
        assertThat(saved.get(1).startSec()).isEqualTo(13.0f);
    }

    @Test
    void 시작_시각이나_턴이_없는_질문은_건너뛴다() {
        Question noStart = question(10L, "재생 안 된 질문", 1, null); // questionStartSec null
        Question noTurn = question(11L, "턴 없는 질문", null, 12.0f);   // turnLevel null
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(noStart, noTurn));

        persister.persist(USER_ID, SESSION_ID);

        verify(utteranceSegmentRepository).deleteBySessionIdAndRole(SESSION_ID, ScriptRole.INTERVIEWER);
        verify(speechToTextTranscriber, never()).transcribe(any());
        verify(utteranceSegmentRepository, never()).saveAll(any(), any(), any());
    }

    @Test
    void 한_질문_변환이_실패해도_다음_질문은_계속_처리한다() {
        Question failing = question(10L, "실패 질문", 1, 12.0f);
        Question ok = question(11L, "정상 질문입니다.", 2, 20.0f);
        given(questionRepository.findAllBySessionId(SESSION_ID)).willReturn(List.of(failing, ok));
        given(interviewVoiceStorage.readBase64(voiceKey(1))).willThrow(new RuntimeException("S3 오류"));
        given(interviewVoiceStorage.readBase64(voiceKey(2)))
                .willReturn(Base64.getEncoder().encodeToString(new byte[]{9}));
        given(speechToTextTranscriber.transcribe(any())).willReturn(new TranscriptionResult(
                "정상 질문입니다.", 1, 0, List.of(new TranscriptSegment("정상 질문입니다.", 0.0f, 1.5f))));

        persister.persist(USER_ID, SESSION_ID);

        // 실패한 질문(10L)은 저장되지 않고, 정상 질문(11L)은 저장된다.
        verify(utteranceSegmentRepository, never()).saveAll(eq(SESSION_ID), eq(10L), any());
        verify(utteranceSegmentRepository).saveAll(eq(SESSION_ID), eq(11L), any());
    }
}

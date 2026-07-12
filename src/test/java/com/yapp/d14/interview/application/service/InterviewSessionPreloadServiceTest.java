package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import com.yapp.d14.interview.application.port.out.JdKeywordExtractor;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.ProbeCandidateExtractor;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.portfolio.application.port.in.PortfolioChunkSearchUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioChunkResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewSessionPreloadServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private PortfolioChunkSearchUseCase portfolioChunkSearchUseCase;

    @Mock
    private JdKeywordExtractor jdKeywordExtractor;

    @Mock
    private ProbeCandidateExtractor probeCandidateExtractor;

    @Mock
    private TextToSpeechSynthesizer textToSpeechSynthesizer;

    @Mock
    private InterviewVoiceStorage interviewVoiceStorage;

    @Mock
    private QuestionCandidateRepository questionCandidateRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewPreloadFailureHandler interviewPreloadFailureHandler;

    @InjectMocks
    private InterviewSessionPreloadService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    private InterviewSession session(String jdUrl, String jdText, String focusProject) {
        return InterviewSession.of(
                1L, userId, portfolioId, JobType.BACKEND, 3, jdUrl, jdText, focusProject,
                InterviewSessionStatus.PREPARING, null, null, null,
                25, 20, 10, 20, 10, 15
        );
    }

    @Test
    void JD_freeText_모두_없으면_JD_키워드_추출_없이_요약_질문을_생성하고_세션을_READY로_전환한다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session(null, null, null)));
        given(portfolioChunkSearchUseCase.searchChunks(eq(portfolioId), any(), anyInt())).willReturn(List.of());
        given(probeCandidateExtractor.extract(any(), any())).willReturn(List.of());
        given(textToSpeechSynthesizer.synthesize(any())).willReturn(null);
        given(interviewSessionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.preload(1L);

        verify(jdKeywordExtractor, never()).extractKeywords(any());
        verify(questionRepository).save(any());
        verify(interviewVoiceStorage, never()).upload(any(), any(), anyInt(), any());
        verify(interviewPreloadFailureHandler, never()).markFailed(any());

        ArgumentCaptor<InterviewSession> captor = ArgumentCaptor.forClass(InterviewSession.class);
        verify(interviewSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InterviewSessionStatus.IN_PROGRESS);
    }

    @Test
    void TTS_합성_결과가_있으면_S3에_업로드하고_그_key를_질문에_저장한다() {
        byte[] audioBytes = {1, 2, 3};
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session(null, null, null)));
        given(portfolioChunkSearchUseCase.searchChunks(eq(portfolioId), any(), anyInt())).willReturn(List.of());
        given(probeCandidateExtractor.extract(any(), any())).willReturn(List.of());
        given(textToSpeechSynthesizer.synthesize(any())).willReturn(audioBytes);
        given(interviewVoiceStorage.upload(userId, 1L, 0, audioBytes)).willReturn("users/x/sessions/1/questions/0.mp3");
        given(interviewSessionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.preload(1L);

        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getAiVoiceS3Key()).isEqualTo("users/x/sessions/1/questions/0.mp3");
    }

    @Test
    void jdText가_있으면_JD_키워드를_추출한다() {
        given(interviewSessionRepository.findById(1L))
                .willReturn(Optional.of(session(null, "JD 원문", null)));
        given(portfolioChunkSearchUseCase.searchChunks(eq(portfolioId), any(), anyInt())).willReturn(List.of());
        given(jdKeywordExtractor.extractKeywords("JD 원문")).willReturn(List.of("키워드1"));
        given(probeCandidateExtractor.extract(any(), any())).willReturn(List.of());
        given(textToSpeechSynthesizer.synthesize(any())).willReturn(null);
        given(interviewSessionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.preload(1L);

        verify(jdKeywordExtractor).extractKeywords("JD 원문");
        verify(probeCandidateExtractor).extract(any(), eq(List.of("키워드1")));
    }

    @Test
    void 추출된_캐물지점_후보를_모두_저장한다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session(null, null, "결제 시스템")));
        given(portfolioChunkSearchUseCase.searchChunks(eq(portfolioId), eq("결제 시스템"), anyInt()))
                .willReturn(List.of(new PortfolioChunkResult("청크1")));
        given(probeCandidateExtractor.extract(any(), any())).willReturn(List.of(
                new ProbeCandidateDraft(TestType.DEPTH, null, "probe", "echo", null, QuestionCandidateStrength.HIGH),
                new ProbeCandidateDraft(TestType.CONFLICT, null, "probe2", "echo2", "키워드", QuestionCandidateStrength.MID)
        ));
        given(textToSpeechSynthesizer.synthesize(any())).willReturn(null);
        given(interviewSessionRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        service.preload(1L);

        ArgumentCaptor<QuestionCandidate> captor = ArgumentCaptor.forClass(QuestionCandidate.class);
        verify(questionCandidateRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(QuestionCandidate::getSource)
                .containsExactly(QuestionCandidateSource.PORTFOLIO, QuestionCandidateSource.JD);
    }

    @Test
    void 처리_중_예외가_발생하면_실패_핸들러가_호출된다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session(null, null, null)));
        given(portfolioChunkSearchUseCase.searchChunks(eq(portfolioId), any(), anyInt()))
                .willThrow(new RuntimeException("포트폴리오 조회 실패"));

        service.preload(1L);

        verify(interviewPreloadFailureHandler).markFailed(1L);
        verify(questionRepository, never()).save(any());
    }

    @Test
    void 세션을_찾을_수_없으면_아무것도_하지_않는다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.empty());

        service.preload(1L);

        verify(interviewPreloadFailureHandler, never()).markFailed(any());
        verify(questionRepository, never()).save(any());
    }
}

package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import com.yapp.d14.interview.application.port.out.CeilingAssessment;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.LiveTurnAnalyzer;
import com.yapp.d14.interview.application.port.out.LiveTurnResult;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.QuestionTextGenerator;
import com.yapp.d14.interview.application.port.out.SpeechToTextTranscriber;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InterviewAnswerSubmitServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewAxisPlanRepository interviewAxisPlanRepository;

    @Mock
    private QuestionCandidateRepository questionCandidateRepository;

    @Mock
    private SpeechToTextTranscriber speechToTextTranscriber;

    @Mock
    private LiveTurnAnalyzer liveTurnAnalyzer;

    @Mock
    private QuestionTextGenerator questionTextGenerator;

    @Mock
    private InterviewAnswerSubmitPersister interviewAnswerSubmitPersister;

    @InjectMocks
    private InterviewAnswerSubmitService service;

    private final UUID userId = UUID.randomUUID();
    private final Long sessionId = 1L;
    private final Long summaryQuestionId = 100L;
    private final byte[] audioContent = "audio".getBytes();

    private InterviewSession session() {
        return InterviewSession.of(
                sessionId, userId, UUID.randomUUID(), JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15
        );
    }

    private Question summaryQuestion() {
        return Question.of(summaryQuestionId, sessionId, "자기소개 부탁드려요", 0, 0, null, null, null, null, null, LocalDateTime.now());
    }

    private List<InterviewAxisPlan> axisPlans() {
        return List.of(
                InterviewAxisPlan.create(sessionId, TestType.DEPTH, AxisTier.CORE, 3),
                InterviewAxisPlan.create(sessionId, TestType.BOUNDARY, AxisTier.CORE, 3),
                InterviewAxisPlan.create(sessionId, TestType.TRADEOFF, AxisTier.CORE, 3),
                InterviewAxisPlan.create(sessionId, TestType.CONNECTION, AxisTier.SUPPORT, 2),
                InterviewAxisPlan.create(sessionId, TestType.CONFLICT, AxisTier.SKIP, 0),
                InterviewAxisPlan.create(sessionId, TestType.RESILIENCE, AxisTier.SKIP, 0)
        );
    }

    private InterviewAnswerSubmitCommand command() {
        return new InterviewAnswerSubmitCommand(sessionId, summaryQuestionId, 0, audioContent, 100f, 110f, 0f, 5f, 5f);
    }

    @Test
    void 정상_흐름이면_다음_질문을_생성하고_결과를_반환한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent)).willReturn("STT 변환된 답변");
        given(liveTurnAnalyzer.analyze(eq(sessionId), any(), eq("STT 변환된 답변"), isNull(), eq(JobType.BACKEND), eq(List.of())))
                .willReturn(new LiveTurnResult(
                        List.of(new ProbeCandidateDraft(TestType.DEPTH, null, "probe", "echo", null, QuestionCandidateStrength.HIGH)),
                        new CeilingAssessment(false, null, "판별 대상 아님"),
                        List.of()
                ));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        QuestionCandidate openCandidate = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.PORTFOLIO, null, TestType.DEPTH, null,
                "probe", "echo", null, QuestionCandidateStrength.HIGH
        );
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH))
                .willReturn(List.of(openCandidate));
        given(questionTextGenerator.generate("probe", "echo")).willReturn("생성된 질문 문장");

        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "생성된 질문 문장", 1, 0, TestType.DEPTH, null, null, null, null, LocalDateTime.now()
        );
        given(interviewAnswerSubmitPersister.persist(any(), any(), any(), any(), anyInt(), any(), any()))
                .willReturn(new InterviewAnswerSubmitPersister.PersistResult(savedAnswer, savedQuestion));

        InterviewAnswerSubmitResult result = service.submit(userId, command());

        assertThat(result.answerId()).isEqualTo(12L);
        assertThat(result.nextQuestion().questionId()).isEqualTo(13L);
        assertThat(result.nextQuestion().isLast()).isFalse();
        assertThat(result.nextQuestion().turnLevel()).isEqualTo(1);
        assertThat(result.nextQuestion().depthLevel()).isEqualTo(0);
        assertThat(result.wrapUpMessage()).isNull();
        assertThat(result.reportId()).isNull();
    }

    @Test
    void 요청의_질문_음성_재생_구간이_답변한_질문에_기록된다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent)).willReturn("STT 변환된 답변");
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(false, null, "판별 대상 아님"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "조금 더 구체적으로 설명해 주실 수 있을까요?", 1, 0, TestType.DEPTH, null, null, null, null, LocalDateTime.now()
        );
        given(interviewAnswerSubmitPersister.persist(any(), any(), any(), any(), anyInt(), any(), any()))
                .willReturn(new InterviewAnswerSubmitPersister.PersistResult(savedAnswer, savedQuestion));

        service.submit(userId, command());

        ArgumentCaptor<Question> answeredQuestionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(interviewAnswerSubmitPersister)
                .persist(any(), answeredQuestionCaptor.capture(), any(), any(), anyInt(), any(), any());
        Question answeredQuestion = answeredQuestionCaptor.getValue();
        assertThat(answeredQuestion.getId()).isEqualTo(summaryQuestionId);
        assertThat(answeredQuestion.getQuestionStartSec()).isEqualTo(100f);
        assertThat(answeredQuestion.getQuestionEndSec()).isEqualTo(110f);
    }

    @Test
    void 후보가_없으면_seed_질문으로_대체하고_질문생성_어댑터는_호출하지_않는다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent)).willReturn("STT 변환된 답변");
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(false, null, "판별 대상 아님"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "조금 더 구체적으로 설명해 주실 수 있을까요?", 1, 0, TestType.DEPTH, null, null, null, null, LocalDateTime.now()
        );
        given(interviewAnswerSubmitPersister.persist(any(), any(), any(), isNull(), anyInt(), any(), any()))
                .willReturn(new InterviewAnswerSubmitPersister.PersistResult(savedAnswer, savedQuestion));

        service.submit(userId, command());

        verify(questionTextGenerator, never()).generate(any(), any());
    }

    @Test
    void 세션이_없으면_예외가_발생하고_이후_단계는_실행되지_않는다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(userId, command()))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND);

        verifyNoInteractions(questionRepository, speechToTextTranscriber, liveTurnAnalyzer);
    }

    @Test
    void 세션_소유자가_아니면_예외가_발생한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));

        assertThatThrownBy(() -> service.submit(UUID.randomUUID(), command()))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INTERVIEW_SESSION_NOT_FOUND);
    }

    @Test
    void 질문이_없으면_예외가_발생한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(userId, command()))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    void CORE_중_가중치가_가장_높은_axis가_선택되고_그_axis에서_jd_match와_strength_우선순위로_probe가_선택된다() {
        // 가중치: depth 20, boundary 30, connection 10, tradeoff 20, conflict 10, resilience 10
        // → CORE(depth/boundary/tradeoff) 중 boundary(30)가 가장 높아 boundary가 선택돼야 한다
        InterviewSession sessionWithBoundaryWeighted = InterviewSession.of(
                sessionId, userId, UUID.randomUUID(), JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                20, 30, 10, 20, 10, 10
        );
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(sessionWithBoundaryWeighted));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent)).willReturn("STT 변환된 답변");
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(false, null, "판별 대상 아님"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());

        // boundary axis의 open 후보 3개 — jd_match 존재 여부 > strength(high>mid>low) 순으로 우선순위가 매겨진다
        QuestionCandidate noJdHighStrength = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.PORTFOLIO, null, TestType.BOUNDARY, null,
                "jd 매칭 없음, strength만 HIGH", "echoA", null, QuestionCandidateStrength.HIGH
        );
        QuestionCandidate jdMatchLowStrength = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.JD, null, TestType.BOUNDARY, null,
                "jd 매칭 있음, strength는 LOW", "echoB", "확장성", QuestionCandidateStrength.LOW
        );
        QuestionCandidate jdMatchHighStrength = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.JD, null, TestType.BOUNDARY, null,
                "jd 매칭 있음, strength도 HIGH", "echoC", "트래픽", QuestionCandidateStrength.HIGH
        );
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.BOUNDARY))
                .willReturn(List.of(noJdHighStrength, jdMatchLowStrength, jdMatchHighStrength));
        given(questionTextGenerator.generate(any(), any())).willReturn("생성된 질문 문장");

        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "생성된 질문 문장", 1, 0, TestType.BOUNDARY, null, null, null, null, LocalDateTime.now()
        );
        given(interviewAnswerSubmitPersister.persist(any(), any(), any(), any(), anyInt(), any(), any()))
                .willReturn(new InterviewAnswerSubmitPersister.PersistResult(savedAnswer, savedQuestion));

        service.submit(userId, command());

        // 1. axis 선택 검증: DEPTH(20)·TRADEOFF(20)가 아니라 BOUNDARY(30)의 open 후보를 조회했다
        verify(questionCandidateRepository).findOpenBySessionIdAndTestType(sessionId, TestType.BOUNDARY);

        // 2. probe 선택 검증: jd_match 없는 HIGH보다, jd_match 있는 후보가 우선이고
        //    그중에서도 strength가 HIGH인 jdMatchHighStrength가 최종 선택된다
        verify(questionTextGenerator).generate("jd 매칭 있음, strength도 HIGH", "echoC");

        ArgumentCaptor<QuestionCandidate> selectedProbeCaptor = ArgumentCaptor.forClass(QuestionCandidate.class);
        verify(interviewAnswerSubmitPersister).persist(any(), any(), any(), selectedProbeCaptor.capture(), anyInt(), any(), any());
        assertThat(selectedProbeCaptor.getValue()).isSameAs(jdMatchHighStrength);
    }

    @Test
    void turnLevel이_0이_아니면_예외가_발생한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        InterviewAnswerSubmitCommand invalidCommand =
                new InterviewAnswerSubmitCommand(sessionId, summaryQuestionId, 1, audioContent, 100f, 110f, 0f, 5f, 5f);

        assertThatThrownBy(() -> service.submit(userId, invalidCommand))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.UNSUPPORTED_TURN_LEVEL);

        verifyNoInteractions(speechToTextTranscriber, liveTurnAnalyzer);
    }
}

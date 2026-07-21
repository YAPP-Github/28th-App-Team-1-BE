package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.command.InterviewAnswerSubmitCommand;
import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.CeilingAssessment;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.LiveTurnAnalyzer;
import com.yapp.d14.interview.application.port.out.LiveTurnResult;
import com.yapp.d14.interview.application.port.out.PriorQaCache;
import com.yapp.d14.interview.application.port.out.PriorTurn;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.QuestionTextGenerator;
import com.yapp.d14.interview.application.port.out.InterviewVoiceStorage;
import com.yapp.d14.interview.application.port.out.SpeechToTextTranscriber;
import com.yapp.d14.interview.application.port.out.TextToSpeechSynthesizer;
import com.yapp.d14.interview.application.port.out.TranscriptionResult;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.CeilingKind;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewEndType;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Base64;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.willThrow;

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
    private AnswerRepository answerRepository;

    @Mock
    private SpeechToTextTranscriber speechToTextTranscriber;

    @Mock
    private LiveTurnAnalyzer liveTurnAnalyzer;

    @Mock
    private QuestionTextGenerator questionTextGenerator;

    @Mock
    private InterviewAnswerSubmitPersister interviewAnswerSubmitPersister;

    @Mock
    private InterviewAnswerTerminationPersister interviewAnswerTerminationPersister;

    @Mock
    private InterviewAnswerAnalyzePersister interviewAnswerAnalyzePersister;

    @Mock
    private InterviewSttResetPersister interviewSttResetPersister;

    @Mock
    private PriorQaCache priorQaCache;

    @Mock
    private InterviewReportGenerateUseCase interviewReportGenerateUseCase;

    @Mock
    private InterviewReportFailureHandler interviewReportFailureHandler;

    @Mock
    private TextToSpeechSynthesizer textToSpeechSynthesizer;

    @Mock
    private InterviewVoiceStorage interviewVoiceStorage;

    @InjectMocks
    private InterviewAnswerSubmitService service;

    private final UUID userId = UUID.randomUUID();
    private final Long sessionId = 1L;
    private final Long summaryQuestionId = 100L;
    private final byte[] audioContent = "audio".getBytes();

    private InterviewSession session() {
        return InterviewSession.of(
                sessionId, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15, 0, 0
        );
    }

    private Question summaryQuestion() {
        return Question.of(summaryQuestionId, sessionId, "자기소개 부탁드려요", 0, 0, null, null, null, null, null, false, LocalDateTime.now());
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
        return new InterviewAnswerSubmitCommand(sessionId, summaryQuestionId, audioContent, 100f, 110f, 0f, 5f, 5f, null, false);
    }

    @Test
    void 정상_흐름이면_다음_질문을_생성하고_결과를_반환한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(liveTurnAnalyzer.analyze(
                eq(sessionId), any(), any(), eq("STT 변환된 답변"), isNull(), eq(JobType.BACKEND), eq(List.of()), eq(List.of())
        ))
                .willReturn(new LiveTurnResult(
                        List.of(new ProbeCandidateDraft(TestType.DEPTH, null, "probe", "echo", null, QuestionCandidateStrength.HIGH, null)),
                        new CeilingAssessment(false, null, "판별 대상 아님"),
                        List.of()
                ));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        QuestionCandidate openCandidate = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.PORTFOLIO, null, TestType.DEPTH, null,
                "probe", "echo", null, QuestionCandidateStrength.HIGH, null
        );
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH))
                .willReturn(List.of(openCandidate));
        given(questionTextGenerator.generate("probe", "echo")).willReturn("생성된 질문 문장");

        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "생성된 질문 문장", 1, 1, TestType.DEPTH, null, null, null, null, false, LocalDateTime.now()
        );
        given(interviewAnswerSubmitPersister.persist(any(), any(), any(), any(), anyInt(), any(), any()))
                .willReturn(new InterviewAnswerSubmitPersister.PersistResult(savedAnswer, savedQuestion));

        InterviewAnswerSubmitResult result = service.submit(userId, command());

        assertThat(result.answerId()).isEqualTo(12L);
        assertThat(result.nextQuestion().questionId()).isEqualTo(13L);
        assertThat(result.nextQuestion().isLast()).isFalse();
        assertThat(result.nextQuestion().turnLevel()).isEqualTo(1);
        assertThat(result.nextQuestion().depthLevel()).isEqualTo(1);
        assertThat(result.wrapUpMessage()).isNull();
        assertThat(result.reportId()).isNull();
    }

    @Test
    void 요청의_질문_음성_재생_구간이_답변한_질문에_기록된다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(false, null, "판별 대상 아님"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "조금 더 구체적으로 설명해 주실 수 있을까요?", 1, 1, TestType.DEPTH, null, null, null, null, false, LocalDateTime.now()
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
    void 재생_구간_시작이_종료보다_크면_400_예외로_변환된다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(false, null, "판별 대상 아님"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        InterviewAnswerSubmitCommand invalidRangeCommand =
                new InterviewAnswerSubmitCommand(sessionId, summaryQuestionId, audioContent, 110f, 100f, 0f, 5f, 5f, null, false);

        assertThatThrownBy(() -> service.submit(userId, invalidRangeCommand))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INVALID_PLAYBACK_RANGE);

        verifyNoInteractions(interviewAnswerSubmitPersister);
    }

    @Test
    void 답변_구간_시작이_종료보다_크면_400_예외로_변환된다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(false, null, "판별 대상 아님"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        InterviewAnswerSubmitCommand invalidRangeCommand =
                new InterviewAnswerSubmitCommand(sessionId, summaryQuestionId, audioContent, 100f, 110f, 5f, 0f, 5f, null, false);

        assertThatThrownBy(() -> service.submit(userId, invalidRangeCommand))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.INVALID_ANSWER_RANGE);

        verifyNoInteractions(interviewAnswerSubmitPersister);
    }

    @Test
    void 후보가_없으면_seed_질문으로_대체하고_질문생성_어댑터는_호출하지_않는다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(false, null, "판별 대상 아님"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "조금 더 구체적으로 설명해 주실 수 있을까요?", 1, 1, TestType.DEPTH, null, null, null, null, false, LocalDateTime.now()
        );
        given(interviewAnswerSubmitPersister.persist(any(), any(), any(), isNull(), anyInt(), any(), any()))
                .willReturn(new InterviewAnswerSubmitPersister.PersistResult(savedAnswer, savedQuestion));

        service.submit(userId, command());

        verify(questionTextGenerator, never()).generate(any(), any());
    }

    @Test
    void 기존_OPEN_후보가_없어도_이번_턴에_추출한_후보로_다음_질문을_생성한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(
                        List.of(new ProbeCandidateDraft(
                                TestType.DEPTH, null, "새로 추출된 probe", "새로 추출된 echo", null, QuestionCandidateStrength.HIGH, null
                        )),
                        new CeilingAssessment(false, null, "판별 대상 아님"),
                        List.of()
                ));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        given(questionTextGenerator.generate("새로 추출된 probe", "새로 추출된 echo")).willReturn("생성된 질문 문장");

        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "생성된 질문 문장", 1, 1, TestType.DEPTH, null, null, null, null, false, LocalDateTime.now()
        );
        given(interviewAnswerSubmitPersister.persist(any(), any(), any(), any(), anyInt(), any(), any()))
                .willReturn(new InterviewAnswerSubmitPersister.PersistResult(savedAnswer, savedQuestion));

        service.submit(userId, command());

        verify(questionTextGenerator).generate("새로 추출된 probe", "새로 추출된 echo");

        ArgumentCaptor<QuestionCandidate> selectedProbeCaptor = ArgumentCaptor.forClass(QuestionCandidate.class);
        verify(interviewAnswerSubmitPersister).persist(any(), any(), any(), selectedProbeCaptor.capture(), anyInt(), any(), any());
        assertThat(selectedProbeCaptor.getValue().getProbeText()).isEqualTo("새로 추출된 probe");
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
    void 이미_종료된_세션이면_예외가_발생하고_이후_단계는_실행되지_않는다() {
        InterviewSession completedSession = InterviewSession.of(
                sessionId, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now(), InterviewEndType.MANUAL_END,
                25, 20, 10, 20, 10, 15, 0, 0
        );
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(completedSession));

        assertThatThrownBy(() -> service.submit(userId, command()))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.SESSION_ALREADY_ENDED);

        verifyNoInteractions(questionRepository, speechToTextTranscriber, liveTurnAnalyzer, interviewAnswerTerminationPersister, interviewReportGenerateUseCase);
    }

    @Test
    void 무효화된_세션이면_예외가_발생하고_이후_단계는_실행되지_않는다() {
        InterviewSession invalidSession = InterviewSession.of(
                sessionId, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.INVALID, LocalDateTime.now(), LocalDateTime.now(), InterviewEndType.MANUAL_END,
                25, 20, 10, 20, 10, 15, 0, 0
        );
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(invalidSession));

        assertThatThrownBy(() -> service.submit(userId, command()))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.SESSION_ALREADY_ENDED);

        verifyNoInteractions(questionRepository, speechToTextTranscriber, liveTurnAnalyzer, interviewAnswerTerminationPersister, interviewReportGenerateUseCase);
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
    void 이미_답변이_있는_질문이면_예외가_발생하고_STT나_분석은_실행되지_않는다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        Answer existingAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "이미 저장된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        given(answerRepository.findByQuestionId(summaryQuestionId)).willReturn(Optional.of(existingAnswer));

        assertThatThrownBy(() -> service.submit(userId, command()))
                .isInstanceOf(InterviewException.class)
                .extracting("errorCode")
                .isEqualTo(InterviewErrorCode.ANSWER_ALREADY_SUBMITTED);

        verifyNoInteractions(speechToTextTranscriber, liveTurnAnalyzer, interviewAnswerSubmitPersister);
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
                sessionId, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                20, 30, 10, 20, 10, 10, 0, 0
        );
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(sessionWithBoundaryWeighted));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(summaryQuestion()));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(false, null, "판별 대상 아님"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());

        // boundary axis의 open 후보 3개 — jd_match 존재 여부 > strength(high>mid>low) 순으로 우선순위가 매겨진다
        QuestionCandidate noJdHighStrength = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.PORTFOLIO, null, TestType.BOUNDARY, null,
                "jd 매칭 없음, strength만 HIGH", "echoA", null, QuestionCandidateStrength.HIGH, null
        );
        QuestionCandidate jdMatchLowStrength = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.JD, null, TestType.BOUNDARY, null,
                "jd 매칭 있음, strength는 LOW", "echoB", "확장성", QuestionCandidateStrength.LOW, null
        );
        QuestionCandidate jdMatchHighStrength = QuestionCandidate.create(
                sessionId, QuestionCandidateSource.JD, null, TestType.BOUNDARY, null,
                "jd 매칭 있음, strength도 HIGH", "echoC", "트래픽", QuestionCandidateStrength.HIGH, null
        );
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.BOUNDARY))
                .willReturn(List.of(noJdHighStrength, jdMatchLowStrength, jdMatchHighStrength));
        given(questionTextGenerator.generate(any(), any())).willReturn("생성된 질문 문장");

        Answer savedAnswer = Answer.of(
                12L, sessionId, summaryQuestionId, "STT 변환된 답변", 0f, 5f, 5f,
                false, null, null, null, null, false, false, null, LocalDateTime.now()
        );
        Question savedQuestion = Question.of(
                13L, sessionId, "생성된 질문 문장", 1, 1, TestType.BOUNDARY, null, null, null, null, false, LocalDateTime.now()
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
    void 종료_사유도_SKIP도_아니면_run_live_turn_결과로_다음_질문을_생성한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 10, 1));
        given(priorQaCache.get(sessionId, TestType.DEPTH)).willReturn(List.of());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        given(liveTurnAnalyzer.analyze(
                eq(sessionId), any(), eq("꼬리 질문"), eq("STT 변환된 답변"), eq(TestType.DEPTH),
                eq(JobType.BACKEND), eq(List.of()), eq(List.of())
        )).willReturn(new LiveTurnResult(
                List.of(new ProbeCandidateDraft(TestType.DEPTH, null, "probe", "echo", null, QuestionCandidateStrength.HIGH, "P3")),
                new CeilingAssessment(false, null, "아직 새 내용이 나오는 중"),
                List.of()
        ));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionTextGenerator.generate("probe", "echo")).willReturn("생성된 꼬리 질문");
        Question savedNextQuestion = Question.of(
                14L, sessionId, "생성된 꼬리 질문", 2, 1, TestType.DEPTH, null, null, null, null, false, LocalDateTime.now()
        );
        given(interviewAnswerAnalyzePersister.persist(
                any(), any(), any(), any(), any(), eq(1), any(), eq(2), any(), isNull(), any()
        )).willReturn(new InterviewAnswerAnalyzePersister.PersistResult(15L, savedNextQuestion));

        InterviewAnswerSubmitResult result = service.submit(userId, command());

        assertThat(result.answerId()).isEqualTo(15L);
        assertThat(result.nextQuestion().questionId()).isEqualTo(14L);
        assertThat(result.nextQuestion().isLast()).isFalse();
        assertThat(result.nextQuestion().turnLevel()).isEqualTo(2);
        assertThat(result.nextQuestion().depthLevel()).isEqualTo(1);
        verify(priorQaCache).append(eq(sessionId), eq(TestType.DEPTH), any());
        verifyNoInteractions(interviewSttResetPersister, interviewAnswerTerminationPersister, interviewReportGenerateUseCase);
        // axis가 전환되지 않으면 이미 조회해둔 OPEN 후보를 재사용해야 한다 — 같은 axis를 두 번 조회하지 않는다.
        verify(questionCandidateRepository, times(1)).findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH);
    }

    @Test
    void isWrapUp이_처음_true로_전환되면_axis_전환_없이_마무리_질문으로_표시한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(priorQaCache.get(sessionId, TestType.DEPTH)).willReturn(List.of());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.DEPTH)).willReturn(List.of());
        given(liveTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(new LiveTurnResult(List.of(), new CeilingAssessment(true, CeilingKind.TOPPED_OUT, "위로 닿음"), List.of()));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        Question savedNextQuestion = Question.of(
                14L, sessionId, "조금 더 구체적으로 설명해 주실 수 있을까요?", 2, 1, TestType.DEPTH, null, null, null, null, true, LocalDateTime.now()
        );
        given(interviewAnswerAnalyzePersister.persist(
                any(), any(), any(), any(), any(), eq(1), any(), eq(2), any(), isNull(), any()
        )).willReturn(new InterviewAnswerAnalyzePersister.PersistResult(17L, savedNextQuestion));

        InterviewAnswerSubmitResult result = service.submit(userId, regularTurnCommand(null, audioContent, true));

        assertThat(result.nextQuestion().isLast()).isTrue();

        ArgumentCaptor<Question> nextQuestionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(interviewAnswerAnalyzePersister)
                .persist(any(), any(), any(), any(), any(), eq(1), any(), eq(2), any(), isNull(), nextQuestionCaptor.capture());
        assertThat(nextQuestionCaptor.getValue().getTestType()).isEqualTo(TestType.DEPTH);
        assertThat(nextQuestionCaptor.getValue().getIsWrapUp()).isTrue();
        verify(questionTextGenerator, never()).generate(any(), any());
    }

    @Test
    void 누적_STT_실패율이_30퍼센트를_초과하면_run_live_turn_없이_세션을_무효화한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 10, 4));
        given(interviewSttResetPersister.persist(any(), any(), any()))
                .willReturn(new InterviewSttResetPersister.PersistResult(42L));

        InterviewAnswerSubmitResult result = service.submit(userId, command());

        assertThat(result.answerId()).isEqualTo(42L);
        assertThat(result.sessionEnded()).isTrue();
        assertThat(result.nextQuestion()).isNull();
        assertThat(result.wrapUpMessage()).isNull();
        verify(priorQaCache).clear(sessionId);
        verifyNoInteractions(liveTurnAnalyzer, interviewAnswerAnalyzePersister, interviewReportGenerateUseCase);
    }

    @Test
    void endType이_SKIP이면_분석_없이_해당_axis_사용량만_소모하고_예산_소진이면_다음_axis로_전환한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(sessionWithTradeoffWeighted()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(interviewAxisPlanRepository.findAllBySessionId(sessionId)).willReturn(axisPlans());
        given(questionCandidateRepository.findOpenBySessionIdAndTestType(sessionId, TestType.TRADEOFF)).willReturn(List.of());
        Question savedNextQuestion = Question.of(
                14L, sessionId, "조금 더 구체적으로 설명해 주실 수 있을까요?", 2, 1, TestType.TRADEOFF, null, null, null, null, false, LocalDateTime.now()
        );
        given(interviewAnswerAnalyzePersister.persistSkipped(any(), any(), isNull(), eq(2), any(), any(), any()))
                .willReturn(new InterviewAnswerAnalyzePersister.PersistResult(16L, savedNextQuestion));

        InterviewAnswerSubmitResult result = service.submit(userId, regularTurnCommand(InterviewEndType.SKIP, null));

        assertThat(result.answerId()).isEqualTo(16L);
        assertThat(result.nextQuestion().questionId()).isEqualTo(14L);
        assertThat(result.nextQuestion().turnLevel()).isEqualTo(2);
        assertThat(result.nextQuestion().depthLevel()).isEqualTo(1);
        verify(interviewAnswerAnalyzePersister).persistSkipped(any(), any(), isNull(), eq(2), any(), any(), any());
        verify(questionTextGenerator, never()).generate(any(), any());
        verifyNoInteractions(speechToTextTranscriber, liveTurnAnalyzer, priorQaCache, interviewSttResetPersister);
    }

    private Question regularQuestion(boolean isWrapUp) {
        return Question.of(
                summaryQuestionId, sessionId, "꼬리 질문", 1, 0, TestType.DEPTH, null, null, null, null, isWrapUp, LocalDateTime.now()
        );
    }

    private InterviewAnswerSubmitCommand regularTurnCommand(InterviewEndType endType, byte[] audio) {
        return regularTurnCommand(endType, audio, false);
    }

    private InterviewAnswerSubmitCommand regularTurnCommand(InterviewEndType endType, byte[] audio, boolean isWrapUp) {
        return new InterviewAnswerSubmitCommand(sessionId, summaryQuestionId, audio, 100f, 110f, 0f, 5f, 5f, endType, isWrapUp);
    }

    private InterviewSession sessionWithTradeoffWeighted() {
        return InterviewSession.of(
                sessionId, userId, UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                20, 15, 10, 30, 10, 15, 0, 0
        );
    }

    @Test
    void endType이_EARLY_EXIT이면_STT만_수행하고_즉시_종료하며_EARLY_EXIT으로_commit한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(interviewAnswerTerminationPersister.persist(any(), any(), any(), any(), any()))
                .willReturn(new InterviewAnswerTerminationPersister.PersistResult(20L));

        InterviewAnswerSubmitResult result = service.submit(userId, regularTurnCommand(InterviewEndType.EARLY_EXIT, audioContent));

        assertThat(result.answerId()).isEqualTo(20L);
        assertThat(result.nextQuestion()).isNull();
        assertThat(result.wrapUpMessage()).isNull();
        assertThat(result.reportId()).isNull();
        verifyNoInteractions(liveTurnAnalyzer);

        ArgumentCaptor<InterviewEndType> endTypeCaptor = ArgumentCaptor.forClass(InterviewEndType.class);
        ArgumentCaptor<String> outcomeReasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(interviewAnswerTerminationPersister)
                .persist(any(), any(), any(), endTypeCaptor.capture(), outcomeReasonCaptor.capture());
        assertThat(endTypeCaptor.getValue()).isEqualTo(InterviewEndType.EARLY_EXIT);
        assertThat(outcomeReasonCaptor.getValue()).isEqualTo("EARLY_EXIT");
        verify(interviewReportGenerateUseCase).generate(sessionId);
    }

    @Test
    void endType이_MANUAL_END이면_짧은_멘트_음성과_함께_종료하고_COMPLETED로_commit한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(interviewVoiceStorage.readBase64(any())).willReturn(null);
        given(textToSpeechSynthesizer.synthesize(any())).willReturn("tts-audio".getBytes());
        given(interviewAnswerTerminationPersister.persist(any(), any(), any(), any(), any()))
                .willReturn(new InterviewAnswerTerminationPersister.PersistResult(21L));

        InterviewAnswerSubmitResult result = service.submit(userId, regularTurnCommand(InterviewEndType.MANUAL_END, audioContent));

        assertThat(result.wrapUpMessage().ttsAudio()).isEqualTo(Base64.getEncoder().encodeToString("tts-audio".getBytes()));
        verify(interviewVoiceStorage).upload(eq("system/interview/wrapup-messages/MANUAL_END.mp3"), any());
        ArgumentCaptor<String> outcomeReasonCaptor = ArgumentCaptor.forClass(String.class);
        verify(interviewAnswerTerminationPersister)
                .persist(any(), any(), any(), eq(InterviewEndType.MANUAL_END), outcomeReasonCaptor.capture());
        assertThat(outcomeReasonCaptor.getValue()).isEqualTo("COMPLETED");
        verify(interviewReportGenerateUseCase).generate(sessionId);

        InOrder order = inOrder(textToSpeechSynthesizer, interviewVoiceStorage, interviewAnswerTerminationPersister);
        order.verify(textToSpeechSynthesizer).synthesize(any());
        order.verify(interviewVoiceStorage).upload(any(), any());
        order.verify(interviewAnswerTerminationPersister).persist(any(), any(), any(), any(), any());
    }

    @Test
    void 마무리_멘트_합성이_실패하면_종료_처리가_저장되지_않아_재요청이_가능하다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(interviewVoiceStorage.readBase64(any())).willReturn(null);
        willThrow(new RuntimeException("TTS 합성 실패")).given(textToSpeechSynthesizer).synthesize(any());

        assertThatThrownBy(() -> service.submit(userId, regularTurnCommand(InterviewEndType.MANUAL_END, audioContent)))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(interviewAnswerTerminationPersister, interviewReportGenerateUseCase);
    }

    @Test
    void 마무리_멘트_음성이_이미_S3에_있으면_재합성하지_않고_캐시를_그대로_쓴다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(interviewVoiceStorage.readBase64("system/interview/wrapup-messages/MANUAL_END.mp3")).willReturn("cached-base64");
        given(interviewAnswerTerminationPersister.persist(any(), any(), any(), any(), any()))
                .willReturn(new InterviewAnswerTerminationPersister.PersistResult(21L));

        InterviewAnswerSubmitResult result = service.submit(userId, regularTurnCommand(InterviewEndType.MANUAL_END, audioContent));

        assertThat(result.wrapUpMessage().ttsAudio()).isEqualTo("cached-base64");
        verifyNoInteractions(textToSpeechSynthesizer);
        verify(interviewVoiceStorage, never()).upload(any(), any());
    }

    @Test
    void endType이_HARD_CAP이고_audio가_없어도_정상적으로_종료된다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(false)));
        given(interviewVoiceStorage.readBase64(any())).willReturn(null);
        given(textToSpeechSynthesizer.synthesize(any())).willReturn("tts-audio".getBytes());
        given(interviewAnswerTerminationPersister.persist(any(), any(), isNull(), any(), any()))
                .willReturn(new InterviewAnswerTerminationPersister.PersistResult(null));

        InterviewAnswerSubmitResult result = service.submit(userId, regularTurnCommand(InterviewEndType.HARD_CAP, null));

        assertThat(result.answerId()).isNull();
        assertThat(result.nextQuestion()).isNull();
        verifyNoInteractions(speechToTextTranscriber);
        verify(interviewAnswerTerminationPersister)
                .persist(any(), any(), isNull(), eq(InterviewEndType.HARD_CAP), eq("COMPLETED"));
    }

    @Test
    void 직전_질문이_isWrapUp이면_자연종료로_처리하고_COMPLETED로_commit한다() {
        given(interviewSessionRepository.findById(sessionId)).willReturn(Optional.of(session()));
        given(questionRepository.findById(summaryQuestionId)).willReturn(Optional.of(regularQuestion(true)));
        given(speechToTextTranscriber.transcribe(audioContent))
                .willReturn(new TranscriptionResult("STT 변환된 답변", 1, 0));
        given(interviewVoiceStorage.readBase64(any())).willReturn(null);
        given(textToSpeechSynthesizer.synthesize(any())).willReturn("tts-audio".getBytes());
        given(interviewAnswerTerminationPersister.persist(any(), any(), any(), any(), any()))
                .willReturn(new InterviewAnswerTerminationPersister.PersistResult(22L));

        InterviewAnswerSubmitResult result = service.submit(userId, regularTurnCommand(null, audioContent));

        assertThat(result.wrapUpMessage()).isNotNull();
        verify(interviewAnswerTerminationPersister)
                .persist(any(), any(), any(), eq(InterviewEndType.NORMAL_END), eq("COMPLETED"));
        verify(interviewReportGenerateUseCase).generate(sessionId);
    }
}

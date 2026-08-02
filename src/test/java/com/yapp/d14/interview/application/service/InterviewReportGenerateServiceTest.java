package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.AxisReportScoreContext;
import com.yapp.d14.interview.application.port.out.AxisReportScorer;
import com.yapp.d14.interview.application.port.out.AxisScoreDraft;
import com.yapp.d14.interview.application.port.out.HeadlineContext;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.RedFlagReconcileContext;
import com.yapp.d14.interview.application.port.out.RedFlagReconciler;
import com.yapp.d14.interview.application.port.out.RedFlagVerdict;
import com.yapp.d14.interview.application.port.out.ReportCardContentContext;
import com.yapp.d14.interview.application.port.out.ReportCardContentGenerator;
import com.yapp.d14.interview.application.port.out.ReportCardDraft;
import com.yapp.d14.interview.application.port.out.ReportHeadlineGenerator;
import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.HeadlineBranch;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.InternalGrade;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStaleReason;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.ticket.application.port.in.TicketCommitUseCase;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewReportGenerateServiceTest {

    @Mock
    private InterviewSessionRepository interviewSessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionCandidateRepository questionCandidateRepository;

    @Mock
    private InterviewAxisPlanRepository interviewAxisPlanRepository;

    @Mock
    private AxisReportScorer axisReportScorer;

    @Mock
    private RedFlagReconciler redFlagReconciler;

    @Mock
    private ReportCardContentGenerator reportCardContentGenerator;

    @Mock
    private ReportHeadlineGenerator reportHeadlineGenerator;

    @Mock
    private InterviewReportPersister interviewReportPersister;

    @Mock
    private InterviewReportFailureHandler interviewReportFailureHandler;

    @Mock
    private QuestionUtteranceSegmentPersister questionUtteranceSegmentPersister;

    @Mock
    private TicketCommitUseCase ticketCommitUseCase;

    @InjectMocks
    private InterviewReportGenerateService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    private InterviewSession session() {
        return InterviewSession.of(
                1L, userId, portfolioId, null, JobType.BACKEND, 3, null, null, null, LocalDateTime.now(),
                InterviewSessionStatus.IN_PROGRESS, LocalDateTime.now(), null, null,
                25, 20, 10, 20, 10, 15, 0, 0, null
        );
    }

    // 재접속 "중단하기"(USER_EXIT)로 트리거된 리포트 생성 — 세션은 ABANDONED라 endType이 없고 abandonCause로 커밋 사유를 판단한다.
    private InterviewSession abandonedSession(AbandonCause cause) {
        return InterviewSession.of(
                1L, userId, portfolioId, null, JobType.BACKEND, 3, null, null, null, LocalDateTime.now(),
                InterviewSessionStatus.ABANDONED, LocalDateTime.now(), LocalDateTime.now(), null,
                25, 20, 10, 20, 10, 15, 0, 0, cause
        );
    }

    private Question question(long id, int turnLevel, TestType testType) {
        return Question.of(
                id, 1L, "질문 " + turnLevel, turnLevel, 1, testType, "principle",
                null, null, null, false, LocalDateTime.now()
        );
    }

    private Answer answer(long id, long questionId, TestType testType, String sttText) {
        return Answer.of(
                id, 1L, questionId, sttText, 0f, 10f, 10f, false, 0f,
                null, null, null, false, false, testType, LocalDateTime.now()
        );
    }

    private InterviewAxisPlan axisPlan(TestType testType, AxisTier tier) {
        return InterviewAxisPlan.of(1L, 1L, testType, tier, 3, 0, false, LocalDateTime.now());
    }

    private QuestionCandidate contradictionCandidate(TestType testType, int originTurn, int contradictingTurn) {
        return QuestionCandidate.of(
                1L, 1L, QuestionCandidateSource.JD, "turn " + originTurn, testType, null,
                "probe", "echo quote", null, QuestionCandidateStrength.MID,
                QuestionCandidateStatus.STALE, QuestionCandidateStaleReason.CONTRADICTED,
                null, contradictingTurn, LocalDateTime.now(), null
        );
    }

    private QuestionCandidate portfolioCandidate(TestType testType) {
        return QuestionCandidate.of(
                1L, 1L, QuestionCandidateSource.PORTFOLIO, "포트폴리오 발췌", testType, null,
                "probe", "echo quote", null, QuestionCandidateStrength.HIGH,
                QuestionCandidateStatus.OPEN, null, 1, null, LocalDateTime.now(), null
        );
    }

    @Test
    void 세션을_찾을_수_없으면_아무것도_하지_않는다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.empty());

        service.generate(1L);

        verify(interviewReportPersister, never()).persist(any(), any(), any(), any(), any());
        verify(interviewReportFailureHandler, never()).markFailed(any());
    }

    @Test
    void 채점할_턴이_없으면_분석_부족으로_즉시_persist하고_AI_포트를_호출하지_않는다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session()));
        given(questionRepository.findAllBySessionId(1L)).willReturn(List.of());
        given(answerRepository.findAllBySessionId(1L)).willReturn(List.of());

        service.generate(1L);

        verify(axisReportScorer, never()).score(any());
        verify(redFlagReconciler, never()).reconcile(any());
        verify(reportCardContentGenerator, never()).generate(any());
        verify(reportHeadlineGenerator, never()).generate(any());

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(interviewReportPersister).persist(eq(1L), reportCaptor.capture(), eq(List.of()), eq(List.of()), eq(List.of()));
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo(ReportStatus.INSUFFICIENT_ANALYSIS);
        assertThat(reportCaptor.getValue().getHeadlineBranch()).isEqualTo(HeadlineBranch.INSUFFICIENT_ANALYSIS);
        verify(ticketCommitUseCase).commit(1L, "COMPLETED");
    }

    @Test
    void ABANDONED_USER_EXIT_세션이면_USER_EXIT_사유로_커밋한다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(abandonedSession(AbandonCause.USER_EXIT)));
        given(questionRepository.findAllBySessionId(1L)).willReturn(List.of());
        given(answerRepository.findAllBySessionId(1L)).willReturn(List.of());

        service.generate(1L);

        verify(ticketCommitUseCase).commit(1L, "USER_EXIT");
    }

    @Test
    void 정상_케이스에서_종합점수를_계산하고_READY로_persist한다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session()));
        given(questionRepository.findAllBySessionId(1L)).willReturn(List.of(
                question(1L, 1, TestType.DEPTH), question(2L, 2, TestType.BOUNDARY)
        ));
        given(answerRepository.findAllBySessionId(1L)).willReturn(List.of(
                answer(1L, 1L, TestType.DEPTH, "깊이 답변"), answer(2L, 2L, TestType.BOUNDARY, "경계 답변")
        ));
        given(questionCandidateRepository.findAllBySessionId(1L)).willReturn(List.of());
        given(interviewAxisPlanRepository.findAllBySessionId(1L)).willReturn(List.of(
                axisPlan(TestType.DEPTH, AxisTier.CORE), axisPlan(TestType.BOUNDARY, AxisTier.SUPPORT)
        ));
        given(axisReportScorer.score(any())).willReturn(List.of(
                new AxisScoreDraft(TestType.DEPTH, 4, ResolutionLevel.NORMAL, null, List.of(), "깊이 근거"),
                new AxisScoreDraft(TestType.BOUNDARY, 3, ResolutionLevel.NORMAL, null, List.of(), "경계 근거")
        ));
        given(reportCardContentGenerator.generate(any())).willReturn(List.of(
                new ReportCardDraft(1L, 1, TestType.DEPTH, "제목", "질문의도", List.of()),
                new ReportCardDraft(2L, 1, TestType.BOUNDARY, "제목2", "질문의도2", List.of())
        ));
        given(reportHeadlineGenerator.generate(any())).willReturn("좋은 답변이었어요");

        service.generate(1L);

        // 후보가 없어도 턴이 있으면 PERFECT_NARRATIVE/BLAME_SHIFTING/BUZZWORD_SALAD 검사를 위해 리컨실러를 1회 호출한다
        verify(redFlagReconciler).reconcile(any());

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AxisEvaluation>> axisCaptor = ArgumentCaptor.forClass(List.class);
        verify(interviewReportPersister).persist(eq(1L), reportCaptor.capture(), axisCaptor.capture(), eq(List.of()), any());

        Report report = reportCaptor.getValue();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.READY);
        assertThat(report.getHeadlineBranch()).isEqualTo(HeadlineBranch.NORMAL);
        assertThat(report.getHeadline()).isEqualTo("좋은 답변이었어요");
        // weightedSum = 25*4 + 20*3 = 160, weightSum = 45 -> 3.56
        assertThat(report.getCompositeScore()).isEqualTo(3.56);
        assertThat(report.getInternalGrade()).isEqualTo(InternalGrade.STRONG_HIRE);
        assertThat(axisCaptor.getValue()).hasSize(2);
        verify(ticketCommitUseCase).commit(1L, "COMPLETED");
    }

    @Test
    void 비노출_레드플래그가_cap을_적용하면_해당_axis_점수가_낮아지고_일반_헤드라인을_유지한다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session()));
        given(questionRepository.findAllBySessionId(1L)).willReturn(List.of(
                question(1L, 1, TestType.DEPTH), question(2L, 2, TestType.BOUNDARY)
        ));
        given(answerRepository.findAllBySessionId(1L)).willReturn(List.of(
                answer(1L, 1L, TestType.DEPTH, "깊이 답변"), answer(2L, 2L, TestType.BOUNDARY, "경계 답변")
        ));
        given(questionCandidateRepository.findAllBySessionId(1L)).willReturn(List.of(
                contradictionCandidate(TestType.DEPTH, 1, 2)
        ));
        given(interviewAxisPlanRepository.findAllBySessionId(1L)).willReturn(List.of(
                axisPlan(TestType.DEPTH, AxisTier.CORE), axisPlan(TestType.BOUNDARY, AxisTier.SUPPORT)
        ));
        given(axisReportScorer.score(any())).willReturn(List.of(
                new AxisScoreDraft(TestType.DEPTH, 4, ResolutionLevel.NORMAL, null, List.of(), "깊이 근거"),
                new AxisScoreDraft(TestType.BOUNDARY, 3, ResolutionLevel.NORMAL, null, List.of(), "경계 근거")
        ));
        given(redFlagReconciler.reconcile(any())).willReturn(List.of(
                new RedFlagVerdict(RedFlagType.BLAME_SHIFTING, TestType.DEPTH, 2, false, List.of(), List.of(), "남탓 근거")
        ));
        given(reportCardContentGenerator.generate(any())).willReturn(List.of());
        given(reportHeadlineGenerator.generate(any())).willReturn("보통 헤드라인");

        service.generate(1L);

        ArgumentCaptor<RedFlagReconcileContext> ctxCaptor = ArgumentCaptor.forClass(RedFlagReconcileContext.class);
        verify(redFlagReconciler).reconcile(ctxCaptor.capture());
        assertThat(ctxCaptor.getValue().contradictionCandidates()).hasSize(1);
        assertThat(ctxCaptor.getValue().contradictionCandidates().get(0).originTurnNumber()).isEqualTo(1);
        assertThat(ctxCaptor.getValue().contradictionCandidates().get(0).contradictingTurnNumber()).isEqualTo(2);

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AxisEvaluation>> axisCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RedFlag>> redFlagCaptor = ArgumentCaptor.forClass(List.class);
        verify(interviewReportPersister).persist(eq(1L), reportCaptor.capture(), axisCaptor.capture(), redFlagCaptor.capture(), any());

        AxisEvaluation depthEvaluation = axisCaptor.getValue().stream()
                .filter(evaluation -> evaluation.getTestType() == TestType.DEPTH)
                .findFirst().orElseThrow();
        assertThat(depthEvaluation.getAppliedCap()).isEqualTo(2);
        assertThat(depthEvaluation.effectiveScore()).isEqualTo(2);
        assertThat(redFlagCaptor.getValue()).hasSize(1);

        Report report = reportCaptor.getValue();
        assertThat(report.getHeadlineBranch()).isEqualTo(HeadlineBranch.NORMAL);
        // weightedSum = 25*2(capped) + 20*3 = 110, weightSum = 45 -> 2.44 -> NO
        assertThat(report.getCompositeScore()).isEqualTo(2.44);
        assertThat(report.getInternalGrade()).isEqualTo(InternalGrade.NO);
    }

    @Test
    void 레드플래그의_relatedTurns가_questionId로_변환되어_저장되고_모르는_턴은_버려진다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session()));
        given(questionRepository.findAllBySessionId(1L)).willReturn(List.of(
                question(1L, 1, TestType.DEPTH), question(2L, 2, TestType.BOUNDARY)
        ));
        given(answerRepository.findAllBySessionId(1L)).willReturn(List.of(
                answer(1L, 1L, TestType.DEPTH, "깊이 답변"), answer(2L, 2L, TestType.BOUNDARY, "경계 답변")
        ));
        given(questionCandidateRepository.findAllBySessionId(1L)).willReturn(List.of());
        given(interviewAxisPlanRepository.findAllBySessionId(1L)).willReturn(List.of(
                axisPlan(TestType.DEPTH, AxisTier.CORE), axisPlan(TestType.BOUNDARY, AxisTier.SUPPORT)
        ));
        given(axisReportScorer.score(any())).willReturn(List.of(
                new AxisScoreDraft(TestType.DEPTH, 4, ResolutionLevel.NORMAL, null, List.of(), "깊이 근거"),
                new AxisScoreDraft(TestType.BOUNDARY, 3, ResolutionLevel.NORMAL, null, List.of(), "경계 근거")
        ));
        given(redFlagReconciler.reconcile(any())).willReturn(List.of(
                // 턴 1·2는 questionId 1L·2L로 매핑되고, 존재하지 않는 턴 99는 버려진다.
                new RedFlagVerdict(RedFlagType.CONTRADICTION, null, null, false, List.of(), List.of(1, 2, 99), "모순 근거")
        ));
        given(reportCardContentGenerator.generate(any())).willReturn(List.of());
        given(reportHeadlineGenerator.generate(any())).willReturn("헤드라인");

        service.generate(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RedFlag>> redFlagCaptor = ArgumentCaptor.forClass(List.class);
        verify(interviewReportPersister).persist(eq(1L), any(), any(), redFlagCaptor.capture(), any());

        RedFlag redFlag = redFlagCaptor.getValue().get(0);
        assertThat(redFlag.getRelatedQuestionIds()).containsExactly(1L, 2L);
    }

    @Test
    void 노출_레드플래그가_knockout이면_등급이_NO_이하로_강등되고_SEVERE_RED_FLAG_헤드라인이_생성된다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session()));
        given(questionRepository.findAllBySessionId(1L)).willReturn(List.of(
                question(1L, 1, TestType.DEPTH), question(2L, 2, TestType.BOUNDARY)
        ));
        given(answerRepository.findAllBySessionId(1L)).willReturn(List.of(
                answer(1L, 1L, TestType.DEPTH, "깊이 답변"), answer(2L, 2L, TestType.BOUNDARY, "경계 답변")
        ));
        given(questionCandidateRepository.findAllBySessionId(1L)).willReturn(List.of(
                portfolioCandidate(TestType.DEPTH)
        ));
        given(interviewAxisPlanRepository.findAllBySessionId(1L)).willReturn(List.of(
                axisPlan(TestType.DEPTH, AxisTier.CORE), axisPlan(TestType.BOUNDARY, AxisTier.SUPPORT)
        ));
        given(axisReportScorer.score(any())).willReturn(List.of(
                new AxisScoreDraft(TestType.DEPTH, 4, ResolutionLevel.NORMAL, null, List.of(), "깊이 근거"),
                new AxisScoreDraft(TestType.BOUNDARY, 3, ResolutionLevel.NORMAL, null, List.of(), "경계 근거")
        ));
        given(redFlagReconciler.reconcile(any())).willReturn(List.of(
                new RedFlagVerdict(RedFlagType.FABRICATION, TestType.DEPTH, null, true, List.of(), List.of(), "날조 근거")
        ));
        given(reportCardContentGenerator.generate(any())).willReturn(List.of());
        given(reportHeadlineGenerator.generate(any())).willReturn("레드플래그 헤드라인");

        service.generate(1L);

        ArgumentCaptor<HeadlineContext> headlineCtxCaptor = ArgumentCaptor.forClass(HeadlineContext.class);
        verify(reportHeadlineGenerator).generate(headlineCtxCaptor.capture());
        assertThat(headlineCtxCaptor.getValue().severeRedFlagPresent()).isTrue();

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(interviewReportPersister).persist(eq(1L), reportCaptor.capture(), any(), any(), any());

        Report report = reportCaptor.getValue();
        assertThat(report.getHeadlineBranch()).isEqualTo(HeadlineBranch.SEVERE_RED_FLAG);
        // cap 없음(raw 3.56) 이지만 knockout으로 NO 이하 강등
        assertThat(report.getInternalGrade()).isEqualTo(InternalGrade.NO);
    }

    @Test
    void 핵심_축이_채점되지_않으면_턴이_있어도_분석_부족으로_처리한다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session()));
        given(questionRepository.findAllBySessionId(1L)).willReturn(List.of(question(1L, 1, TestType.DEPTH)));
        given(answerRepository.findAllBySessionId(1L)).willReturn(List.of(answer(1L, 1L, TestType.DEPTH, "깊이 답변")));
        given(questionCandidateRepository.findAllBySessionId(1L)).willReturn(List.of());
        given(interviewAxisPlanRepository.findAllBySessionId(1L)).willReturn(List.of(
                axisPlan(TestType.DEPTH, AxisTier.CORE), axisPlan(TestType.CONFLICT, AxisTier.CORE)
        ));
        given(axisReportScorer.score(any())).willReturn(List.of(
                new AxisScoreDraft(TestType.DEPTH, 4, ResolutionLevel.NORMAL, null, List.of(), "깊이 근거")
        ));
        given(reportCardContentGenerator.generate(any())).willReturn(List.of(
                new ReportCardDraft(1L, 1, TestType.DEPTH, "제목", "질문의도", List.of())
        ));

        service.generate(1L);

        verify(reportHeadlineGenerator, never()).generate(any());

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(interviewReportPersister).persist(eq(1L), reportCaptor.capture(), any(), any(), any());
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo(ReportStatus.INSUFFICIENT_ANALYSIS);
        assertThat(reportCaptor.getValue().getHeadlineBranch()).isEqualTo(HeadlineBranch.INSUFFICIENT_ANALYSIS);
    }

    @Test
    void AI_호출이_재시도_후에도_실패하면_실패_핸들러가_호출되고_persist는_호출되지_않는다() {
        given(interviewSessionRepository.findById(1L)).willReturn(Optional.of(session()));
        given(questionRepository.findAllBySessionId(1L)).willReturn(List.of(question(1L, 1, TestType.DEPTH)));
        given(answerRepository.findAllBySessionId(1L)).willReturn(List.of(answer(1L, 1L, TestType.DEPTH, "깊이 답변")));
        given(questionCandidateRepository.findAllBySessionId(1L)).willReturn(List.of());
        given(interviewAxisPlanRepository.findAllBySessionId(1L)).willReturn(List.of(axisPlan(TestType.DEPTH, AxisTier.CORE)));
        given(axisReportScorer.score(any())).willThrow(new RuntimeException("LLM 호출 실패"));

        service.generate(1L);

        verify(interviewReportFailureHandler).markFailed(1L);
        verify(interviewReportPersister, never()).persist(any(), any(), any(), any(), any());
        verify(ticketCommitUseCase, never()).commit(any(), any());
    }
}

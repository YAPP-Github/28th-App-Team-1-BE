package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.AxisEvaluationRepository;
import com.yapp.d14.interview.application.port.out.AxisReportScorer;
import com.yapp.d14.interview.application.port.out.AxisScoreDraft;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.RedFlagReconciler;
import com.yapp.d14.interview.application.port.out.RedFlagRepository;
import com.yapp.d14.interview.application.port.out.ReportCardContentGenerator;
import com.yapp.d14.interview.application.port.out.ReportCardDraft;
import com.yapp.d14.interview.application.port.out.ReportCardRepository;
import com.yapp.d14.interview.application.port.out.ReportHeadlineGenerator;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.application.port.out.RedFlagVerdict;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.HeadlineBranch;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TextRange;
import com.yapp.d14.interview.domain.TimeRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Tag("integration")
@SpringBootTest
class InterviewReportGenerateServiceIntegrationTest {

    @Autowired
    private InterviewReportGenerateUseCase interviewReportGenerateUseCase;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private InterviewAxisPlanRepository interviewAxisPlanRepository;

    @Autowired
    private QuestionCandidateRepository questionCandidateRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AxisEvaluationRepository axisEvaluationRepository;

    @Autowired
    private RedFlagRepository redFlagRepository;

    @Autowired
    private ReportCardRepository reportCardRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private AxisReportScorer axisReportScorer;

    @MockitoBean
    private RedFlagReconciler redFlagReconciler;

    @MockitoBean
    private ReportCardContentGenerator reportCardContentGenerator;

    @MockitoBean
    private ReportHeadlineGenerator reportHeadlineGenerator;

    private Long sessionId;

    @AfterEach
    void cleanUp() {
        if (sessionId == null) {
            return;
        }
        // deleteBySessionId(derived delete query)는 트랜잭션 밖에서 호출하면
        // "No EntityManager with actual transaction available" 예외를 던지므로 TransactionTemplate로 감싼다.
        // @ElementCollection 자식 테이블까지 포함한 삭제는 프로덕션과 동일하게 repository에 위임하고,
        // 삭제 포트가 없는 Answer/InterviewAxisPlan/InterviewSession만 JdbcTemplate로 직접 지운다.
        transactionTemplate.executeWithoutResult(status -> {
            reportRepository.deleteBySessionId(sessionId);
            axisEvaluationRepository.deleteBySessionId(sessionId);
            redFlagRepository.deleteBySessionId(sessionId);
            reportCardRepository.deleteBySessionId(sessionId);
            questionCandidateRepository.deleteBySessionId(sessionId);
            questionRepository.deleteBySessionId(sessionId);
            jdbcTemplate.update("DELETE FROM answer WHERE session_id = ?", sessionId);
            jdbcTemplate.update("DELETE FROM interview_axis_plan WHERE session_id = ?", sessionId);
            jdbcTemplate.update("DELETE FROM interview_session WHERE id = ?", sessionId);
        });
    }

    private Long createSession() {
        InterviewSession saved = interviewSessionRepository.save(InterviewSession.of(
                null, UUID.randomUUID(), UUID.randomUUID(), null, JobType.BACKEND, 3, null, null, null,
                InterviewSessionStatus.IN_PROGRESS, null, null, null,
                25, 20, 10, 20, 10, 15, 0, 0
        ));
        return saved.getId();
    }

    private Long createQuestion(Long sessionId, int turnLevel, TestType testType, String content, String appliedPrinciple) {
        return questionRepository.save(
                Question.create(sessionId, content, turnLevel, 1, testType, appliedPrinciple, null, false)
        ).getId();
    }

    private void createAnswer(Long sessionId, Long questionId, TestType testType, String sttText) {
        answerRepository.save(Answer.create(
                sessionId, questionId, sttText, 0f, 10f, 10f, false, 0f, null, null, null, false, false, testType
        ));
    }

    private void createAxisPlan(Long sessionId, TestType testType, AxisTier tier) {
        interviewAxisPlanRepository.save(InterviewAxisPlan.create(sessionId, testType, tier, 3));
    }

    @Test
    void 실제_DB에_저장된_fixture로_파이프라인을_돌리면_영속화_계층까지_왕복한다() {
        sessionId = createSession();
        Long depthQuestionId = createQuestion(
                sessionId, 1, TestType.DEPTH,
                "포트폴리오에서 언급하신 결제 재시도 로직에 대해 더 깊이 설명해주실 수 있나요? 왜 그 방식을 선택하셨나요?",
                "P3"
        );
        Long boundaryQuestionId = createQuestion(
                sessionId, 2, TestType.BOUNDARY,
                "그 결제 시스템이 처리하던 트래픽 규모는 어느 정도였고, 어떤 지점에서 한계를 느끼셨나요?",
                "P5"
        );
        createAnswer(sessionId, depthQuestionId, TestType.DEPTH,
                "결제 API가 타임아웃될 때 중복 결제를 막기 위해 요청 UUID를 멱등키로 발급하고, "
                        + "실패 시 exponential backoff로 최대 3번까지 재시도하도록 구현했습니다. "
                        + "멱등키는 Redis에 TTL 10분으로 저장해서 같은 요청이 중복 처리되지 않도록 걸러냈습니다.");
        createAnswer(sessionId, boundaryQuestionId, TestType.BOUNDARY,
                "피크 시간대 기준 초당 200건 정도의 결제 요청을 처리했는데, "
                        + "Redis 커넥션 풀이 부족해서 커넥션 타임아웃이 자주 발생했습니다. "
                        + "풀 사이즈를 늘리고 커넥션 재사용 방식을 바꿔서 해결했습니다.");
        createAxisPlan(sessionId, TestType.DEPTH, AxisTier.CORE);
        createAxisPlan(sessionId, TestType.BOUNDARY, AxisTier.SUPPORT);

        given(axisReportScorer.score(any())).willReturn(List.of(
                new AxisScoreDraft(
                        TestType.DEPTH, 4, ResolutionLevel.NORMAL, null,
                        List.of(new TimeRange(12.0f, 38.0f)),
                        "멱등키 발급 방식과 재시도 정책을 구체적인 수치와 함께 설명해 근본 원인까지 답변함"
                ),
                new AxisScoreDraft(
                        TestType.BOUNDARY, 3, ResolutionLevel.NORMAL, null,
                        List.of(), "트래픽 규모와 병목 지점을 파악하고 있으나 대안 비교는 부족함"
                )
        ));
        given(reportCardContentGenerator.generate(any())).willReturn(List.of(
                new ReportCardDraft(
                        depthQuestionId, 1, TestType.DEPTH, "이 질문은 실패 상황에서 데이터 정합성을 어떻게 보장했는지 확인하려는 의도예요.",
                        List.of(new HighlightSpan(
                                new TextRange(0, 30), HighlightTone.GOOD,
                                "요청 UUID를 멱등키로 발급해 재시도 시 중복 결제를 방지한 구체적인 근거를 제시했습니다.",
                                List.of("그 멱등키의 TTL은 어떻게 잡았고, 만료 후 재요청이 오면 어떻게 되나요?")
                        ))
                ),
                new ReportCardDraft(
                        boundaryQuestionId, 1, TestType.BOUNDARY, "이 질문은 실제 트래픽 규모와 병목 지점을 정확히 파악하고 있는지 확인하려는 의도예요.",
                        List.of()
                )
        ));
        given(reportHeadlineGenerator.generate(any())).willReturn("결제 재시도 로직을 깊이 있게 설명했고, 트래픽 규모에 따른 한계도 잘 짚었어요.");

        interviewReportGenerateUseCase.generate(sessionId);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(reportRepository.findBySessionId(sessionId)).isPresent()
        );

        Report report = reportRepository.findBySessionId(sessionId).orElseThrow();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.READY);
        assertThat(report.getHeadlineBranch()).isEqualTo(HeadlineBranch.NORMAL);
        assertThat(report.getHeadline()).isEqualTo("결제 재시도 로직을 깊이 있게 설명했고, 트래픽 규모에 따른 한계도 잘 짚었어요.");
        assertThat(report.getCompositeScore()).isEqualTo(3.56);

        List<AxisEvaluation> axisEvaluations = axisEvaluationRepository.findAllBySessionId(sessionId);
        assertThat(axisEvaluations).hasSize(2);
        AxisEvaluation depthEvaluation = axisEvaluations.stream()
                .filter(evaluation -> evaluation.getTestType() == TestType.DEPTH)
                .findFirst().orElseThrow();
        assertThat(depthEvaluation.getEvidenceTimestamps()).containsExactly(new TimeRange(12.0f, 38.0f));

        List<ReportCard> reportCards = reportCardRepository.findAllBySessionId(sessionId);
        assertThat(reportCards).hasSize(2);
        ReportCard depthCard = reportCards.stream()
                .filter(card -> card.getTestType() == TestType.DEPTH)
                .findFirst().orElseThrow();
        assertThat(depthCard.getHighlightSpans()).hasSize(1);
        assertThat(depthCard.getHighlightSpans().get(0).tone()).isEqualTo(HighlightTone.GOOD);
    }

    @Test
    void 노출_레드플래그_knockout이_실제_DB에서도_등급을_NO로_강등시킨다() {
        sessionId = createSession();
        Long depthQuestionId = createQuestion(
                sessionId, 1, TestType.DEPTH,
                "포트폴리오에 결제 시스템 아키텍처를 처음부터 설계하고 구현을 리드했다고 적으셨는데, "
                        + "구체적으로 어떤 부분을 직접 설계하셨나요?",
                "P3"
        );
        Long boundaryQuestionId = createQuestion(
                sessionId, 2, TestType.BOUNDARY,
                "그 결제 시스템이 처리하던 트래픽 규모는 어느 정도였나요?",
                "P5"
        );
        createAnswer(sessionId, depthQuestionId, TestType.DEPTH,
                "음... 팀에서 다 같이 논의해서 정한 거라 제가 어떤 부분을 맡았는지는 정확히 기억이 잘 안 나요. "
                        + "전체적인 구조는 알고 있습니다.");
        createAnswer(sessionId, boundaryQuestionId, TestType.BOUNDARY,
                "초당 200건 정도였던 것 같습니다.");
        createAxisPlan(sessionId, TestType.DEPTH, AxisTier.CORE);
        createAxisPlan(sessionId, TestType.BOUNDARY, AxisTier.SUPPORT);
        questionCandidateRepository.save(QuestionCandidate.create(
                sessionId, QuestionCandidateSource.PORTFOLIO,
                "포트폴리오 3페이지 결제 시스템 프로젝트 항목", TestType.DEPTH, null,
                "포트폴리오에 결제 시스템 아키텍처를 처음부터 설계하고 구현을 리드했다고 적으셨는데, "
                        + "구체적으로 어떤 부분을 직접 설계하셨나요?",
                "결제 시스템 아키텍처를 처음부터 설계하고 구현을 리드했습니다.",
                null, QuestionCandidateStrength.HIGH, null
        ));

        given(axisReportScorer.score(any())).willReturn(List.of(
                new AxisScoreDraft(TestType.DEPTH, 4, ResolutionLevel.NORMAL, null, List.of(),
                        "설계 주도 경험을 구체적으로 설명하지 못하고 팀 전체 의사결정으로 답변을 흐림"),
                new AxisScoreDraft(TestType.BOUNDARY, 3, ResolutionLevel.NORMAL, null, List.of(),
                        "트래픽 규모는 언급했으나 근거 수치가 불명확함")
        ));
        given(redFlagReconciler.reconcile(any())).willReturn(List.of(
                new RedFlagVerdict(
                        RedFlagType.FABRICATION, TestType.DEPTH, null, true, List.of(),
                        "포트폴리오에서는 아키텍처 설계를 리드했다고 주장했지만, "
                                + "실제 답변에서는 본인이 맡은 부분을 특정하지 못하고 회피성 답변으로 일관함"
                )
        ));
        given(reportCardContentGenerator.generate(any())).willReturn(List.of());
        given(reportHeadlineGenerator.generate(any())).willReturn(
                "포트폴리오에 기재된 경험과 실제 답변 사이에 확인이 필요한 불일치가 있어요."
        );

        interviewReportGenerateUseCase.generate(sessionId);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(reportRepository.findBySessionId(sessionId)).isPresent()
        );

        Report report = reportRepository.findBySessionId(sessionId).orElseThrow();
        assertThat(report.getHeadlineBranch()).isEqualTo(HeadlineBranch.SEVERE_RED_FLAG);
        assertThat(report.getInternalGrade().name()).isEqualTo("NO");

        List<RedFlag> redFlags = redFlagRepository.findAllBySessionId(sessionId);
        assertThat(redFlags).hasSize(1);
        assertThat(redFlags.get(0).isKnockout()).isTrue();
    }
}

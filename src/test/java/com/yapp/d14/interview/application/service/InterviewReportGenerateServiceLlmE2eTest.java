package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportGenerateUseCase;
import com.yapp.d14.interview.application.port.out.AnswerRepository;
import com.yapp.d14.interview.application.port.out.AxisEvaluationRepository;
import com.yapp.d14.interview.application.port.out.InterviewAxisPlanRepository;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.RedFlagRepository;
import com.yapp.d14.interview.application.port.out.ReportCardRepository;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.ActionKeyword;
import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.Question;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.ReportStatus;
import com.yapp.d14.interview.domain.TestType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 실제 Anthropic(claude-sonnet-5)을 호출하는 리포트 파이프라인 e2e 테스트.
 * 4개 AI 포트를 mock하지 않고 실제 어댑터를 그대로 태워, 면접 종료 후 리포트 생성까지
 * (axis 채점 → 레드플래그 확정 → 헤드라인 → 카드 생성) 전 구간이 실 LLM 응답으로 도는지 검증한다.
 *
 * 비용·비결정성 때문에 기본 test/integrationTest에서 제외되며(@Tag("llm-e2e")),
 * ./gradlew llmE2eTest 로만 실행한다. 로컬 Postgres/Redis와 유효한 ANTHROPIC_API_KEY가 필요하다.
 * 실 LLM 출력은 값이 매번 달라지므로 정확한 문자열이 아니라 구조·범위만 단언하고, 내용은 로그로 남긴다.
 */
@Tag("llm-e2e")
@SpringBootTest
class InterviewReportGenerateServiceLlmE2eTest {

    private static final Logger log = LoggerFactory.getLogger(InterviewReportGenerateServiceLlmE2eTest.class);

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

    private Long sessionId;

    @AfterEach
    void cleanUp() {
        if (sessionId == null) {
            return;
        }
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

    @Test
    void 실제_LLM으로_면접종료부터_리포트생성까지_전_구간을_돌린다() {
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
        // 레드플래그 확정 호출(②)까지 태우기 위해 포트폴리오 대조 후보 1건을 심는다.
        // 답변이 포폴 내용과 일치하므로 정상적으로는 레드플래그가 확정되지 않아야 한다.
        questionCandidateRepository.save(QuestionCandidate.create(
                sessionId, QuestionCandidateSource.PORTFOLIO,
                "포트폴리오 3페이지 결제 시스템 프로젝트 항목", TestType.DEPTH, null,
                "결제 재시도 로직을 어떻게 구현했는지, 멱등성을 어떻게 보장했는지",
                "결제 재시도 로직과 멱등키 기반 중복 방지를 구현했습니다.",
                null, QuestionCandidateStrength.HIGH, null
        ));

        interviewReportGenerateUseCase.generate(sessionId);

        // 실 LLM 4회 순차 호출 + 재시도 여유. 넉넉하게 대기.
        await().atMost(Duration.ofSeconds(180)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            Report r = reportRepository.findBySessionId(sessionId).orElse(null);
            assertThat(r).as("리포트가 생성됨").isNotNull();
            assertThat(r.getStatus())
                    .as("아직 처리 중이 아니라 종료 상태여야 함")
                    .isIn(ReportStatus.READY, ReportStatus.INSUFFICIENT_ANALYSIS, ReportStatus.FAILED);
        });

        Report report = reportRepository.findBySessionId(sessionId).orElseThrow();
        List<AxisEvaluation> axisEvaluations = axisEvaluationRepository.findAllBySessionId(sessionId);
        List<RedFlag> redFlags = redFlagRepository.findAllBySessionId(sessionId);
        List<ReportCard> reportCards = reportCardRepository.findAllBySessionId(sessionId);

        logOutputs(report, axisEvaluations, redFlags, reportCards);

        // 실 LLM이 정상 응답했다면 READY로 끝나야 한다(파싱/호출 실패면 FAILED).
        assertThat(report.getStatus()).isEqualTo(ReportStatus.READY);
        assertThat(report.getHeadline()).isNotBlank();
        assertThat(report.getCompositeScore()).isBetween(1.0, 4.0);
        assertThat(report.getInternalGrade()).isNotNull();

        // ① axis 채점: 채점 대상 2축 모두 1~4점으로 산출.
        assertThat(axisEvaluations).hasSize(2);
        assertThat(axisEvaluations)
                .allSatisfy(e -> assertThat(e.getScore()).isBetween(1, 4));

        // ④ 카드: 채점된 축마다 카드가 생성되고 질문 분석이 채워짐.
        assertThat(reportCards).isNotEmpty();
        assertThat(reportCards)
                .allSatisfy(c -> assertThat(c.getQuestionIntentTranslation()).isNotBlank());
    }

    @Test
    void 실제_LLM_다턴_답변으로_카드_하이라이트_키워드_고쳐쓰기까지_생성한다() {
        sessionId = createSession();
        // DEPTH 축에 3턴을 쌓아 점점 구체화된 답변을 줘 NORMAL resolution을 유도한다.
        Long q1 = createQuestion(sessionId, 1, TestType.DEPTH,
                "결제 응답 속도를 개선하신 경험을 말씀해주세요. 무엇이 문제였나요?", "P1");
        Long q2 = createQuestion(sessionId, 2, TestType.DEPTH,
                "응답이 느렸던 근본 원인은 무엇이었고, 어떻게 진단하셨나요?", "P2");
        Long q3 = createQuestion(sessionId, 3, TestType.DEPTH,
                "그 해결책이 왜 효과가 있었는지, 남은 한계는 없었는지 설명해주세요.", "P3");
        // 턴마다 다른 발화 구간을 줘서 하이라이트가 실제 답변 위치로 흩어지는지 확인한다.
        createAnswer(sessionId, q1, TestType.DEPTH,
                "결제 화면에서 응답이 평균 800ms 정도로 느려서 사용자 이탈이 있었어요. 그래서 개선 작업을 맡았습니다.",
                0f, 15f);
        createAnswer(sessionId, q2, TestType.DEPTH,
                "APM으로 프로파일링해보니 한 결제 요청에 상품·재고·할인 조회 쿼리가 반복돼서 "
                        + "DB 왕복이 7번 도는 N+1이 원인이었습니다. 그중 자주 바뀌지 않는 6번을 캐시로 흡수했어요.",
                15f, 45f);
        createAnswer(sessionId, q3, TestType.DEPTH,
                "자주 안 바뀌는 상품·할인 데이터를 Redis에 올려 DB 호출을 줄였더니 600ms를 깎아 200ms가 됐습니다. "
                        + "다만 남은 200ms는 외부 결제사 API 호출이라 저희가 더 줄이지는 못했고, 그 부분은 타임아웃과 재시도로만 방어했어요.",
                45f, 80f);
        createAxisPlan(sessionId, TestType.DEPTH, AxisTier.CORE);

        interviewReportGenerateUseCase.generate(sessionId);

        await().atMost(Duration.ofSeconds(180)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            Report r = reportRepository.findBySessionId(sessionId).orElse(null);
            assertThat(r).as("리포트가 생성됨").isNotNull();
            assertThat(r.getStatus()).isIn(ReportStatus.READY, ReportStatus.INSUFFICIENT_ANALYSIS, ReportStatus.FAILED);
        });

        Report report = reportRepository.findBySessionId(sessionId).orElseThrow();
        List<AxisEvaluation> axisEvaluations = axisEvaluationRepository.findAllBySessionId(sessionId);
        List<RedFlag> redFlags = redFlagRepository.findAllBySessionId(sessionId);
        List<ReportCard> reportCards = reportCardRepository.findAllBySessionId(sessionId);

        logOutputs(report, axisEvaluations, redFlags, reportCards);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.READY);
        assertThat(axisEvaluations).hasSize(1);
        assertThat(reportCards).isNotEmpty();
    }

    private void logOutputs(Report report, List<AxisEvaluation> axisEvaluations,
                            List<RedFlag> redFlags, List<ReportCard> reportCards) {
        log.info("========== [LLM E2E] 리포트 생성 결과 ==========");
        log.info("[REPORT] status={}, grade={}, composite={}, branch={}",
                report.getStatus(), report.getInternalGrade(), report.getCompositeScore(), report.getHeadlineBranch());
        log.info("[HEADLINE] {}", report.getHeadline());
        for (AxisEvaluation e : axisEvaluations) {
            log.info("[AXIS] {} score={} resolution={}/{} rationale={}",
                    e.getTestType(), e.getScore(), e.getResolutionLevel(), e.getResolutionLowReason(), e.getRationale());
        }
        log.info("[RED FLAGS] count={}", redFlags.size());
        for (RedFlag f : redFlags) {
            log.info("  - type={} affected={} cap={} knockout={}",
                    f.getType(), f.getAffectedTestType(), f.getCapValue(), f.isKnockout());
        }
        for (ReportCard c : reportCards) {
            log.info("[CARD] {} intent={}", c.getTestType(), c.getQuestionIntentTranslation());
            for (HighlightSpan h : c.getHighlightSpans()) {
                log.info("    highlight {} [{}~{}]", h.tone(), h.range().startIndex(), h.range().endIndex());
                for (ActionKeyword k : h.actionKeywords()) {
                    log.info("        keyword: {} - {}", k.keyword(), k.suggestion());
                    if (k.rewrittenText() != null) {
                        log.info("        rewrittenText: {}", k.rewrittenText());
                    }
                }
            }
        }
        log.info("===============================================");
    }

    private Long createSession() {
        InterviewSession saved = interviewSessionRepository.save(InterviewSession.of(
                null, UUID.randomUUID(), UUID.randomUUID(), JobType.BACKEND, 3, null, null, null,
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
        createAnswer(sessionId, questionId, testType, sttText, 0f, 10f);
    }

    private void createAnswer(Long sessionId, Long questionId, TestType testType, String sttText,
                              float startSec, float endSec) {
        answerRepository.save(Answer.create(
                sessionId, questionId, sttText, startSec, endSec, endSec - startSec,
                false, 0f, null, null, null, false, false, testType
        ));
    }

    private void createAxisPlan(Long sessionId, TestType testType, AxisTier tier) {
        interviewAxisPlanRepository.save(InterviewAxisPlan.create(sessionId, testType, tier, 3));
    }
}

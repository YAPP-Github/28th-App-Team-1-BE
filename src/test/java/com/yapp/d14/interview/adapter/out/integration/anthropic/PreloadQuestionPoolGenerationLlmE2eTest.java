package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.in.InterviewSessionPreloadUseCase;
import com.yapp.d14.interview.application.port.out.InterviewSessionRepository;
import com.yapp.d14.interview.application.port.out.QuestionCandidateRepository;
import com.yapp.d14.interview.application.port.out.QuestionRepository;
import com.yapp.d14.interview.application.port.out.QuestionTextGenerator;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.QuestionCandidate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 이슈 #67: 실제 preload(포트폴리오 청크 검색 + AnthropicProbeCandidateExtractorAdapter 캐물지점 추출)로
 * 후보풀을 만든 뒤, 각 후보를 QuestionTextGenerator로 바로 질문 문장화해 레이턴시·질문 퀄리티를
 * 눈으로 확인하는 e2e 테스트.
 *
 * 요약 질문에 대한 답변 제출 단계는 건너뛴다 — AnthropicLiveTurnAnalyzerAdapter는 "이미 있는 답변"을
 * 분석해 새 캐물지점을 뽑는 어댑터라 답변 없이는 호출 대상이 없고, preload 후보를 질문 문장으로
 * 바꾸는 역할은 QuestionTextGenerator가 담당한다.
 *
 * 로컬 Postgres/Redis, 실제 임베딩이 끝난 portfolioId, 유효한 OPENAI_API_KEY·ANTHROPIC_API_KEY가 필요하다.
 * 비용·비결정성 때문에 기본 test/integrationTest에서 제외되며(@Tag("llm-e2e")), ./gradlew llmE2eTest로만 실행한다.
 */
@Tag("llm-e2e")
@SpringBootTest
class PreloadQuestionPoolGenerationLlmE2eTest {

    private static final Logger log = LoggerFactory.getLogger(PreloadQuestionPoolGenerationLlmE2eTest.class);
    private static final int CAREER_YEARS = 3;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private QuestionCandidateRepository questionCandidateRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private InterviewSessionPreloadUseCase interviewSessionPreloadUseCase;

    @Autowired
    private QuestionTextGenerator questionTextGenerator;

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
            questionCandidateRepository.deleteBySessionId(sessionId);
            questionRepository.deleteBySessionId(sessionId);
            jdbcTemplate.update("DELETE FROM interview_session WHERE id = ?", sessionId);
        });
        sessionId = null;
    }

    static Stream<Arguments> portfolioScenarios() {
        return Stream.of(
                Arguments.of("user1-BACKEND", "00000000-0000-0000-0000-000000000001",
                        "fb7f2a69-b31d-4c43-8c44-fa2f0f41492f", JobType.BACKEND),
                Arguments.of("user2-BACKEND", "00000000-0000-0000-0000-000000000002",
                        "e60b3b41-bd1c-4846-b6dd-15e196a28905", JobType.BACKEND),
                Arguments.of("user3-IOS", "00000000-0000-0000-0000-000000000003",
                        "f48120b4-5272-4b46-b9ce-30319d8cac84", JobType.IOS),
                Arguments.of("user4-IOS", "00000000-0000-0000-0000-000000000004",
                        "e98c8816-c5d5-4a47-878a-f073f2ac90b3", JobType.IOS),
                Arguments.of("user5-ANDROID", "00000000-0000-0000-0000-000000000005",
                        "4b23917f-621c-48a9-bdf1-2c1173b5d3ef", JobType.ANDROID),
                Arguments.of("user6-FRONTEND", "00000000-0000-0000-0000-000000000006",
                        "b1922625-7918-4b78-9968-bae9cf6a602c", JobType.FRONTEND)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("portfolioScenarios")
    void 실제_preload_후보풀로_다음_면접_질문을_생성한다(String label, String userId, String portfolioId, JobType jobRole) {
        log.info("========== [LLM E2E] {} 시작 (jobRole={}) ==========", label, jobRole);

        Instant preloadStartedAt = Instant.now();
        sessionId = createSession(UUID.fromString(userId), UUID.fromString(portfolioId), jobRole);
        interviewSessionPreloadUseCase.preload(sessionId);

        await().atMost(Duration.ofSeconds(120)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            InterviewSession session = interviewSessionRepository.findById(sessionId).orElseThrow();
            assertThat(session.getStatus())
                    .isIn(InterviewSessionStatus.IN_PROGRESS, InterviewSessionStatus.PRELOAD_FAILED);
        });
        double preloadElapsedSeconds = elapsedSeconds(preloadStartedAt);

        InterviewSession session = interviewSessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getStatus())
                .as("preload가 실패하지 않고 READY(IN_PROGRESS)로 전환되어야 함")
                .isEqualTo(InterviewSessionStatus.IN_PROGRESS);

        List<QuestionCandidate> candidates = questionCandidateRepository.findAllBySessionId(sessionId);
        log.info("[{}] preload 완료: elapsedSeconds={}, candidateCount={}", label, preloadElapsedSeconds, candidates.size());
        assertThat(candidates).as("포트폴리오 청크에서 캐물지점 후보가 최소 1개 이상 나와야 함").isNotEmpty();

        for (QuestionCandidate candidate : candidates) {
            Instant questionStartedAt = Instant.now();
            String questionText = questionTextGenerator.generate(candidate.getProbeText(), candidate.getEchoQuote());
            double questionElapsedSeconds = elapsedSeconds(questionStartedAt);

            log.info("  - axis={} strength={} elapsedSeconds={}",
                    candidate.getTestType(), candidate.getStrength(), questionElapsedSeconds);
            log.info("    probeText={}", candidate.getProbeText());
            log.info("    echoQuote={}", candidate.getEchoQuote());
            log.info("    -> 질문: {}", questionText);

            assertThat(questionText).isNotBlank();
        }

        log.info("========== [LLM E2E] {} 종료 (총 {}건) ==========", label, candidates.size());
    }

    private Long createSession(UUID userId, UUID portfolioId, JobType jobRole) {
        InterviewSession session = InterviewSession.create(
                userId, portfolioId, jobRole, CAREER_YEARS, null, null, null
        );
        return interviewSessionRepository.save(session).getId();
    }

    private double elapsedSeconds(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis() / 1000.0;
    }
}

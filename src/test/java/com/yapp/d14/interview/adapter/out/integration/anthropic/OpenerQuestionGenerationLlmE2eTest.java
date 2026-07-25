package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #45 seed(오프너) 질문 생성 로직이 실제 Anthropic 호출에서 직무·연차·axis별로
 * 어떻게 다른 문구를 만들어내는지 눈으로 확인하기 위한 e2e 테스트.
 *
 * 비용·비결정성 때문에 기본 test/integrationTest에서 제외되며(@Tag("llm-e2e")),
 * ./gradlew llmE2eTest 로만 실행한다.
 */
@Tag("llm-e2e")
class OpenerQuestionGenerationLlmE2eTest {

    private static final Logger log = LoggerFactory.getLogger(OpenerQuestionGenerationLlmE2eTest.class);

    @Test
    void 실제_LLM으로_직군_axis_조합별_여는_질문을_생성한다() {
        ChatModel chatModel = buildRealAnthropicChatModel();
        AnthropicQuestionTextGeneratorAdapter questionTextGenerator = new AnthropicQuestionTextGeneratorAdapter(chatModel);

        List<JobType> jobTypes = List.of(JobType.BACKEND, JobType.FRONTEND, JobType.INFRA_SRE);
        List<TestType> axes = List.of(TestType.CONFLICT, TestType.RESILIENCE, TestType.TRADEOFF);
        int yearsOfExperience = 3;

        log.info("========== [LLM E2E] 여는 질문(opener) 직군×axis 조합 ==========");
        for (JobType jobType : jobTypes) {
            for (TestType axis : axes) {
                String opener = questionTextGenerator.generateOpener(axis, jobType, yearsOfExperience, List.of(), List.of());
                log.info("[직무={} / 연차={}년 / axis={}] {}", jobType.getLabel(), yearsOfExperience, axis.getLabel(), opener);
                assertThat(opener).isNotBlank();
            }
        }
        log.info("================================================================");
    }

    @Test
    void 실제_LLM으로_JD_포폴_소재가_뒷받침될_때와_안될_때_조건부_오프너를_비교한다() {
        ChatModel chatModel = buildRealAnthropicChatModel();
        AnthropicQuestionTextGeneratorAdapter questionTextGenerator = new AnthropicQuestionTextGeneratorAdapter(chatModel);

        List<String> jdKeywords = List.of("대용량 트래픽 처리", "Redis 캐시 전략");

        log.info("========== [LLM E2E] 조건부 오프너: 포폴 근거 있음/없음 비교 ==========");

        List<String> groundedChunks = List.of(
                "이커머스 주문 서비스에서 타임세일 트래픽 급증 시 Redis 기반 분산락과 로컬 캐시를 도입해 " +
                        "응답 지연을 800ms에서 200ms로 줄였다."
        );
        String groundedOpener = questionTextGenerator.generateOpener(
                TestType.DEPTH, JobType.BACKEND, 3, jdKeywords, groundedChunks
        );
        log.info("[근거 있음] {}", groundedOpener);
        assertThat(groundedOpener).isNotBlank();

        String ungroundedOpener = questionTextGenerator.generateOpener(
                TestType.DEPTH, JobType.BACKEND, 3, jdKeywords, List.of()
        );
        log.info("[근거 없음(포폴 청크 미제공)] {}", ungroundedOpener);
        assertThat(ungroundedOpener).isNotBlank();

        log.info("================================================================");
    }

    @Test
    void 실제_LLM으로_직군별_조건부_오프너_근거_유무를_비교한다() {
        ChatModel chatModel = buildRealAnthropicChatModel();
        AnthropicQuestionTextGeneratorAdapter questionTextGenerator = new AnthropicQuestionTextGeneratorAdapter(chatModel);

        List<JobTypeFixture> fixtures = List.of(
                new JobTypeFixture(
                        JobType.BACKEND,
                        List.of("대용량 트래픽 처리", "Redis 캐시 전략"),
                        "이커머스 주문 서비스에서 타임세일 트래픽 급증 시 Redis 기반 분산락과 로컬 캐시를 도입해 " +
                                "응답 지연을 800ms에서 200ms로 줄였다."
                ),
                new JobTypeFixture(
                        JobType.FRONTEND,
                        List.of("웹 성능 최적화", "번들 사이즈 최적화"),
                        "커머스 상세페이지에서 이미지 지연 로딩과 라우트 단위 코드 스플리팅을 도입해 " +
                                "LCP를 4.2초에서 1.8초로, 초기 번들 크기를 40% 줄였다."
                ),
                new JobTypeFixture(
                        JobType.IOS,
                        List.of("메모리 관리", "앱 크래시율 개선"),
                        "iOS 앱에서 retain cycle로 인한 메모리 누수를 Instruments로 추적해 해결하고, " +
                                "크래시율을 2%에서 0.3%로 낮췄다."
                ),
                new JobTypeFixture(
                        JobType.ANDROID,
                        List.of("앱 시작 속도 개선", "ANR 감소"),
                        "안드로이드 앱의 콜드 스타트 시간을 초기화 로직 지연 로딩으로 3.5초에서 1.2초로 줄이고, " +
                                "메인 스레드 블로킹을 없애 ANR 발생률을 낮췄다."
                ),
                new JobTypeFixture(
                        JobType.DATA_ENGINEER,
                        List.of("대용량 배치 처리", "데이터 파이프라인 최적화"),
                        "일 배치로 도는 ETL 파이프라인의 병목을 Spark 파티셔닝 전략으로 개선해 " +
                                "처리 시간을 6시간에서 40분으로 줄였다."
                ),
                new JobTypeFixture(
                        JobType.INFRA_SRE,
                        List.of("장애 대응", "SLA 99.9% 유지"),
                        "온콜 대응 중 반복되던 서비스 장애를 자동화된 롤백 파이프라인 구축으로 " +
                                "평균 복구 시간을 30분에서 5분으로 줄였다."
                )
        );
        int yearsOfExperience = 3;

        log.info("========== [LLM E2E] 직군별 조건부 오프너: 포폴 근거 있음/없음 비교 ==========");
        for (JobTypeFixture fixture : fixtures) {
            String grounded = questionTextGenerator.generateOpener(
                    TestType.DEPTH, fixture.jobType(), yearsOfExperience, fixture.jdKeywords(), List.of(fixture.groundedChunk())
            );
            log.info("[{} / 근거 있음] {}", fixture.jobType().getLabel(), grounded);
            assertThat(grounded).isNotBlank();

            String ungrounded = questionTextGenerator.generateOpener(
                    TestType.DEPTH, fixture.jobType(), yearsOfExperience, fixture.jdKeywords(), List.of()
            );
            log.info("[{} / 근거 없음] {}", fixture.jobType().getLabel(), ungrounded);
            assertThat(ungrounded).isNotBlank();
        }
        log.info("================================================================");
    }

    private record JobTypeFixture(JobType jobType, List<String> jdKeywords, String groundedChunk) {
    }

    private ChatModel buildRealAnthropicChatModel() {
        AnthropicApi anthropicApi = AnthropicApi.builder()
                .apiKey(readAnthropicApiKeyFromEnvFile())
                .build();

        AnthropicChatOptions defaultOptions = AnthropicChatOptions.builder()
                .model("claude-haiku-4-5-20251001")
                .maxTokens(8192)
                .thinking(AnthropicApi.ThinkingType.DISABLED, null)
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(defaultOptions)
                .build();
    }

    // Spring Boot 컨텍스트 없이(DB 불필요) 도는 테스트라 spring-dotenv 자동 주입을 못 받는다.
    // 프로젝트 루트 .env를 직접 읽어 ANTHROPIC_API_KEY만 꺼내온다.
    private String readAnthropicApiKeyFromEnvFile() {
        try {
            Path envPath = Path.of(".env");
            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            return lines.stream()
                    .filter(line -> line.startsWith("ANTHROPIC_API_KEY="))
                    .map(line -> line.substring("ANTHROPIC_API_KEY=".length()).trim())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(".env에 ANTHROPIC_API_KEY가 없어요."));
        } catch (IOException e) {
            throw new IllegalStateException(".env 파일을 읽지 못했어요. 프로젝트 루트에서 실행 중인지 확인하세요.", e);
        }
    }
}

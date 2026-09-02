package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.common.metrics.AiCallMetrics;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
 * 이전 면접 질문 이력을 프롬프트에 넘겼을 때 실제로 중복을 피하는지 실제 Anthropic 호출로 확인한다(#133).
 * 이력을 안 넘긴 baseline과 넘긴 결과를 나란히 찍어 눈으로도 비교할 수 있게 한다.
 *
 * 비용·비결정성 때문에 기본 test/integrationTest에서 제외되며(@Tag("llm-e2e")),
 * ./gradlew llmE2eTest 로만 실행한다.
 */
@Tag("llm-e2e")
class PriorQuestionDedupLlmE2eTest {

    private static final Logger log = LoggerFactory.getLogger(PriorQuestionDedupLlmE2eTest.class);

    private static final List<String> PORTFOLIO_CHUNKS = List.of(
            "이커머스 주문 서비스에서 타임세일 트래픽 급증 시 Redis 기반 분산락을 도입해 재고 초과 판매를 막고 "
                    + "응답 지연을 800ms에서 200ms로 줄였다.",
            "결제 실패 건을 모아 재처리하는 배치를 만들면서, 재시도 주기와 멱등키 설계를 두고 정산팀과 협의했다."
    );

    @Test
    void 이전_질문을_넘기면_여는_질문이_그_소재를_피한다() {
        AnthropicQuestionTextGeneratorAdapter generator =
                new AnthropicQuestionTextGeneratorAdapter(buildRealAnthropicChatModel(), new AnthropicUsageRecorder(command -> {
                }), new AiCallMetrics(new SimpleMeterRegistry()));

        List<String> priorQuestions = List.of(
                "일하면서 예상과 다르게 흘러갔거나 실패했던 경험, 그리고 거기서 무엇을 배우셨는지 말씀해 주실 수 있을까요?",
                "실패한 경험에서 배운 점을 이후 업무에 어떻게 적용하셨나요?"
        );

        String baseline = generator.generateOpener(
                1L,
                TestType.RESILIENCE, JobType.BACKEND, 3, List.of(), List.of(), List.of()
        );
        String deduped = generator.generateOpener(
                1L,
                TestType.RESILIENCE, JobType.BACKEND, 3, List.of(), List.of(), priorQuestions
        );

        log.info("========== [LLM E2E] 여는 질문 중복 회피 ==========");
        log.info("[이전 질문] {}", priorQuestions);
        log.info("[이력 미전달] {}", baseline);
        log.info("[이력 전달  ] {}", deduped);
        log.info("==================================================");

        assertThat(baseline).isNotBlank();
        assertThat(deduped).isNotBlank();
        assertThat(deduped)
                .as("이전 질문과 똑같은 문장을 그대로 다시 내면 안 된다")
                .isNotEqualTo(priorQuestions.get(0));
    }

    @Test
    void 이전_질문이_다룬_지점은_후보풀에서_빠지고_안_다룬_지점이_올라온다() {
        AnthropicProbeCandidateExtractorAdapter extractor =
                new AnthropicProbeCandidateExtractorAdapter(buildRealAnthropicChatModel(), new AnthropicUsageRecorder(command -> {
                }), new AiCallMetrics(new SimpleMeterRegistry()));

        List<String> priorQuestions = List.of(
                "타임세일 트래픽이 몰릴 때 Redis 분산락을 왜 그 방식으로 잡으셨는지 궁금해요.",
                "분산락을 도입하면서 재고 초과 판매를 어떻게 막으셨나요?",
                "응답 지연을 800ms에서 200ms로 줄이신 과정을 좀 더 설명해 주실 수 있을까요?"
        );

        List<ProbeCandidateDraft> baseline = extractor.extract(1L, null, PORTFOLIO_CHUNKS, List.of(), List.of());
        List<ProbeCandidateDraft> deduped = extractor.extract(1L, null, PORTFOLIO_CHUNKS, List.of(), priorQuestions);

        log.info("========== [LLM E2E] 후보풀 중복 회피 ==========");
        log.info("[이전 질문] {}", priorQuestions);
        logDrafts("이력 미전달", baseline);
        logDrafts("이력 전달  ", deduped);
        log.info("===============================================");

        assertThat(baseline).isNotEmpty();
        assertThat(deduped).isNotEmpty();
        assertThat(joined(deduped))
                .as("이전 질문이 안 다룬 결제 재처리 배치 쪽 지점이 후보로 올라와야 한다")
                .containsAnyOf("재처리", "멱등", "재시도", "정산", "결제");
    }

    private void logDrafts(String label, List<ProbeCandidateDraft> drafts) {
        log.info("[{}] {}건", label, drafts.size());
        for (ProbeCandidateDraft draft : drafts) {
            log.info("  - axis={}, strength={}, probeText={}", draft.testType(), draft.strength(), draft.probeText());
        }
    }

    private String joined(List<ProbeCandidateDraft> drafts) {
        return drafts.stream().map(this::text).reduce("", (a, b) -> a + "\n" + b);
    }

    private String text(ProbeCandidateDraft draft) {
        return (draft.probeText() == null ? "" : draft.probeText())
                + " " + (draft.echoQuote() == null ? "" : draft.echoQuote());
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

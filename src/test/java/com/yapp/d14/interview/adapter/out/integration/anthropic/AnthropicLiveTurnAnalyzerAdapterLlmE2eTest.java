package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.LiveTurnResult;
import com.yapp.d14.interview.application.port.out.PriorQaCache;
import com.yapp.d14.interview.application.port.out.PriorTurn;
import com.yapp.d14.interview.application.port.out.StaleProbeUpdate;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * turnLevel≥1 일반화된 run_live_turn을 실제 Anthropic(Haiku)로 태워, ceiling·stale_updates가
 * 실제로 동작하는지 눈으로 확인하기 위한 e2e 테스트. DB 없이 어댑터를 직접 생성해서 호출한다.
 * 비용·비결정성 때문에 기본 test에서 제외되며(@Tag("llm-e2e")), ./gradlew llmE2eTest 로만 실행한다.
 */
@Tag("llm-e2e")
class AnthropicLiveTurnAnalyzerAdapterLlmE2eTest {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLiveTurnAnalyzerAdapterLlmE2eTest.class);

    @Test
    void 이전_캐물지점과_모순되는_답변이면_stale_updates로_감지한다() {
        ChatModel chatModel = buildRealAnthropicChatModel();
        AnthropicLiveTurnAnalyzerAdapter liveTurnAnalyzer = new AnthropicLiveTurnAnalyzerAdapter(
                chatModel, (portfolioId, queryText, topK) -> List.of(), new NoOpPriorQaCache()
        );

        QuestionCandidate openProbe = QuestionCandidate.of(
                99L, 1L, QuestionCandidateSource.ANSWER, "턴 2", TestType.TRADEOFF, null,
                "이 결정을 혼자 내렸는지 확인", "그건 제가 혼자 판단해서 결정했어요", null, QuestionCandidateStrength.HIGH,
                QuestionCandidateStatus.OPEN, null, null, null, LocalDateTime.now(), null
        );

        String lastQuestion = "방금 말씀하신 기술 선택은 누구와 논의해서 결정하신 건가요?";
        String lastAnswer = "사실 그건 저 혼자 결정한 게 아니라, 팀 리드님과 상의해서 함께 결정한 거였어요.";

        LiveTurnResult result = liveTurnAnalyzer.analyze(
                1L, null, lastQuestion, lastAnswer, TestType.TRADEOFF, JobType.BACKEND,
                List.of(new PriorTurn(2, "그 기술을 선택하신 이유가 뭔가요?", "혼자 판단해서 결정했어요", TestType.TRADEOFF)),
                List.of(openProbe)
        );

        log.info("========== [LLM E2E] run_live_turn(turnLevel>=1) 결과 ==========");
        log.info("[ceiling] {}", result.ceiling());
        log.info("[stale_updates {}건]", result.staleUpdates().size());
        for (StaleProbeUpdate update : result.staleUpdates()) {
            log.info("  - probeId={} reason={} flagRef={}", update.probeId(), update.reason(), update.flagRef());
        }
        log.info("[new_probes {}건]", result.newProbes().size());
        log.info("===================================================");

        assertThat(result.ceiling()).isNotNull();
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

    private static class NoOpPriorQaCache implements PriorQaCache {
        @Override
        public List<PriorTurn> get(Long sessionId, TestType axis) {
            return List.of();
        }

        @Override
        public void append(Long sessionId, TestType axis, PriorTurn turn) {
        }

        @Override
        public void clear(Long sessionId) {
        }
    }
}

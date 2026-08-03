package com.yapp.d14.interview.adapter.out.integration.anthropic;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.anthropic.api.AnthropicCacheOptions;
import org.springframework.ai.anthropic.api.AnthropicCacheStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #79: Spring AI 1.1.x 업그레이드로 도입한 AnthropicCacheOptions(SYSTEM_ONLY)가 run_live_turn 시스템 프롬프트
 * (axes.yaml+principles.yaml+ceiling-fewshot.md, 매 턴 동일하게 재사용됨)에 실제로 효과가 있는지 turn별 처리
 * latency로 눈으로 확인하기 위한 e2e 테스트. 같은 시스템 프롬프트로 5턴을 연속 호출해, 캐시 미적용(매번 전체 재처리) 대비
 * 캐시 적용(2턴째부터 캐시 히트) 시 지연시간이 줄어드는지 로그로 비교한다.
 * 비용·비결정성 때문에 기본 test에서 제외되며(@Tag("llm-e2e")), ./gradlew llmE2eTest 로만 실행한다.
 */
@Tag("llm-e2e")
class LiveTurnCachingLatencyLlmE2eTest {

    private static final Logger log = LoggerFactory.getLogger(LiveTurnCachingLatencyLlmE2eTest.class);
    private static final int TURN_COUNT = 5;

    @Test
    void 캐싱_미적용과_적용의_turn별_latency를_비교한다() {
        ChatModel chatModel = buildRealAnthropicChatModel();
        String systemPrompt = buildSystemPrompt();

        List<Long> uncachedLatenciesMs = runTurns(chatModel, systemPrompt, false);
        List<Long> cachedLatenciesMs = runTurns(chatModel, systemPrompt, true);

        log.info("========== [LLM E2E] run_live_turn 프롬프트 캐싱 latency 비교 (turn당 ms) ==========");
        log.info("[캐싱 미적용] {}", uncachedLatenciesMs);
        log.info("[캐싱 적용]   {}", cachedLatenciesMs);
        log.info("[캐싱 미적용 평균(2턴째부터)] {}", average(uncachedLatenciesMs.subList(1, uncachedLatenciesMs.size())));
        log.info("[캐싱 적용 평균(2턴째부터)]   {}", average(cachedLatenciesMs.subList(1, cachedLatenciesMs.size())));
        log.info("=====================================================================");

        assertThat(uncachedLatenciesMs).hasSize(TURN_COUNT);
        assertThat(cachedLatenciesMs).hasSize(TURN_COUNT);
    }

    // 매 턴 다른 답변 내용을 넣어 실제 사용 패턴처럼 user 메시지는 바뀌되, system 프롬프트는 동일하게 유지한다
    // (캐시 히트 조건 — system 프롬프트가 바뀌지 않아야 한다).
    private List<Long> runTurns(ChatModel chatModel, String systemPrompt, boolean withCaching) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        AnthropicChatOptions options = withCaching ? cachedOptions(chatModel) : null;

        List<Long> latenciesMs = new ArrayList<>();
        for (int turn = 1; turn <= TURN_COUNT; turn++) {
            String userMessage = buildUserMessage(turn);
            long start = System.nanoTime();
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt().system(systemPrompt).user(userMessage);
            if (options != null) {
                spec = spec.options(options);
            }
            ChatResponse response = spec.call().chatResponse();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            latenciesMs.add(elapsedMs);
            AnthropicApi.Usage usage = (AnthropicApi.Usage) response.getMetadata().getUsage().getNativeUsage();
            log.info("[{}] turn={} elapsedMs={} inputTokens={} cacheCreationInputTokens={} cacheReadInputTokens={}",
                    withCaching ? "캐싱 적용" : "캐싱 미적용", turn, elapsedMs,
                    usage.inputTokens(), usage.cacheCreationInputTokens(), usage.cacheReadInputTokens());
        }
        return latenciesMs;
    }

    private double average(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private AnthropicChatOptions cachedOptions(ChatModel chatModel) {
        AnthropicChatOptions options = AnthropicChatOptions.fromOptions((AnthropicChatOptions) chatModel.getDefaultOptions());
        options.setCacheOptions(AnthropicCacheOptions.builder().strategy(AnthropicCacheStrategy.SYSTEM_ONLY).build());
        return options;
    }

    private String buildUserMessage(int turn) {
        return """
                [직무] BACKEND
                [current_axis] depth
                [완료된 axis] 없음
                [방금 질문] %d번째 질문입니다. 방금 작업하신 내용을 더 자세히 설명해주시겠어요?
                [방금 답변] 네, %d번째 턴 답변입니다. Redis 기반 분산락과 요청 UUID 기반 멱등키를 도입해서
                동일 주문 건의 중복 결제를 막았고, 결제 실패율을 낮췄습니다.
                [prior_qa]
                없음
                [open_probes]
                없음
                """.formatted(turn, turn);
    }

    private String buildSystemPrompt() {
        return """
                당신은 AI 면접관입니다. 지원자의 방금 답변을 분석해 캐물지점 후보를 추출하고 천장을 판별합니다.

                아래 6대 평가 항목(axis) 정의를 기준으로 각 캐물지점에 axis 태그를 답니다.
                %s

                아래는 캐물지점을 만들 때 참고할 전술 목록(P1~P24)과 직군별 필수/권장 매트릭스입니다.
                %s

                아래는 천장 판별 기준을 잡아주는 예시입니다.
                %s

                출력은 다른 설명 없이 아래 스키마의 JSON 객체 하나만 반환하세요:
                {
                  "newProbes": [{axis, secondaryAxis, probeText, echoQuote, jdMatch, strength, principleUsed}, ...],
                  "ceiling": {reached, kind, reason},
                  "staleUpdates": [{probeId, reason, flagRef}, ...]
                }
                """.formatted(loadResource("interview-rubric/axes.yaml"),
                loadResource("interview-rubric/principles.yaml"),
                loadResource("interview-rubric/ceiling-fewshot.md"));
    }

    private String loadResource(String path) {
        try {
            return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(path + " 로드에 실패했어요.", e);
        }
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
}

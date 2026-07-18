package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.LiveTurnResult;
import com.yapp.d14.interview.application.port.out.PriorQaCache;
import com.yapp.d14.interview.application.port.out.PriorTurn;
import com.yapp.d14.interview.application.port.out.ProbeCandidateDraft;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #35 turnLevel=0(요약 답변) 특수 처리 경로 중 run_live_turn → generate_question_text
 * 두 Anthropic(Haiku) 어댑터를 실제로 태워, 백엔드 개발자의 자기소개 답변에서 다음 AI 질문이
 * 어떻게 만들어지는지 눈으로 확인하기 위한 e2e 테스트.
 *
 * InterviewAnswerSubmitServiceTest(mock)에서 이미 axis/probe 선택 우선순위 로직은 검증했으므로,
 * 여기서는 그 앞단인 "실제 LLM이 답변에서 무엇을 추출하고, 그걸로 어떤 질문 문장을 만드는지"만 본다.
 * DB(Postgres) 없이 두 어댑터를 직접 생성해서 호출하므로 로컬 인프라 없이도 ANTHROPIC_API_KEY만 있으면 돈다.
 *
 * 비용·비결정성 때문에 기본 test/integrationTest에서 제외되며(@Tag("llm-e2e")),
 * ./gradlew llmE2eTest 로만 실행한다.
 */
@Tag("llm-e2e")
class FirstTurnQuestionGenerationLlmE2eTest {

    private static final Logger log = LoggerFactory.getLogger(FirstTurnQuestionGenerationLlmE2eTest.class);

    @Test
    void 실제_LLM으로_백엔드_개발자_자기소개_답변에서_다음_질문을_생성한다() {
        ChatModel chatModel = buildRealAnthropicChatModel();
        AnthropicLiveTurnAnalyzerAdapter liveTurnAnalyzer = new AnthropicLiveTurnAnalyzerAdapter(
                chatModel, (portfolioId, queryText, topK) -> List.of(), new NoOpPriorQaCache()
        );
        AnthropicQuestionTextGeneratorAdapter questionTextGenerator = new AnthropicQuestionTextGeneratorAdapter(chatModel);

        String summaryQuestion = "가장 자신 있는 프로젝트를 2분간 소개해주세요.";
        String selfIntroduction = """
                안녕하세요, 저는 3년 차 백엔드 개발자입니다. 최근에는 이커머스 플랫폼에서
                주문·결제 서비스를 담당했는데, 타임세일 때 트래픽이 몰리면서 같은 주문이
                중복 결제되는 문제가 있었습니다. 이를 해결하기 위해 Redis 기반 분산락과
                요청 UUID 기반 멱등키를 도입해서 동일 주문 건의 중복 결제를 막았고,
                Spring Boot와 JPA로 결제 도메인을 재설계하면서 트랜잭션 경계도 명확히
                나눴습니다. 그 결과 결제 실패율을 0.8%에서 0.1%로 줄였고, 피크 시간대
                기준 초당 200건까지도 안정적으로 처리할 수 있게 됐습니다.
                """;

        // 1. run_live_turn: 첫 턴이라 current_axis=null, prior_qa=[]로 호출
        LiveTurnResult liveTurnResult = liveTurnAnalyzer.analyze(
                1L, null, summaryQuestion, selfIntroduction, null, JobType.BACKEND, List.of(), List.of()
        );

        log.info("========== [LLM E2E] run_live_turn 결과 ==========");
        log.info("[답변] {}", selfIntroduction.replaceAll("\\s+", " ").trim());
        log.info("[추출된 캐물지점 {}건]", liveTurnResult.newProbes().size());
        for (ProbeCandidateDraft probe : liveTurnResult.newProbes()) {
            log.info("  - axis={} strength={} jdMatch={} probeText={} echoQuote={}",
                    probe.testType(), probe.strength(), probe.jdMatch(), probe.probeText(), probe.echoQuote());
        }
        assertThat(liveTurnResult.newProbes()).isNotEmpty();

        // 2. select_next_probe 없이(DB 없음) strength가 가장 높은 후보 하나를 골라 generate_question_text 호출
        ProbeCandidateDraft topProbe = pickHighestStrength(liveTurnResult.newProbes());
        String nextQuestionText = questionTextGenerator.generate(topProbe.probeText(), topProbe.echoQuote());

        log.info("[선택된 캐물지점] axis={} probeText={} echoQuote={}",
                topProbe.testType(), topProbe.probeText(), topProbe.echoQuote());
        log.info("[다음 AI 질문] {}", nextQuestionText);
        log.info("===================================================");

        assertThat(nextQuestionText).isNotBlank();
    }

    private ProbeCandidateDraft pickHighestStrength(List<ProbeCandidateDraft> probes) {
        Optional<ProbeCandidateDraft> best = probes.stream()
                .max(Comparator.comparing(p -> switch (p.strength()) {
                    case HIGH -> 2;
                    case MID -> 1;
                    case LOW -> 0;
                }));
        return best.orElseThrow();
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

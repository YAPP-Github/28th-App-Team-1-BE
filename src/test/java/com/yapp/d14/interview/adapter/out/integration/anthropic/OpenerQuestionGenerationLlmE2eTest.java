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
                String opener = questionTextGenerator.generateOpener(axis, jobType, yearsOfExperience);
                log.info("[직무={} / 연차={}년 / axis={}] {}", jobType.getLabel(), yearsOfExperience, axis.getLabel(), opener);
                assertThat(opener).isNotBlank();
            }
        }
        log.info("================================================================");
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

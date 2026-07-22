package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.QuestionTextGenerator;
import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.TestType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
class AnthropicQuestionTextGeneratorAdapter implements QuestionTextGenerator {

    private static final String AXES_YAML_PATH = "interview-rubric/axes.yaml";

    private static final String SYSTEM_PROMPT = """
            당신은 AI 면접관입니다. 입력으로 캐물 의도(probeText, 내부 메모)와
            되받아 물을 원 표현(echoQuote, 지원자가 실제로 한 말)을 받습니다.

            이 둘을 자연스러운 구어체 질문 문장 하나로 바꾸세요.
            - echoQuote를 질문 안에서 그대로 되받아 물어 지원자가 자기 말이 이어지고 있다고 느끼게 합니다.
            - probeText는 질문에 그대로 노출하지 말고, 그 의도를 자연스러운 질문으로 녹여냅니다.
            - 대본을 읽는 듯한 딱딱한 어투가 아니라 실제 면접관이 대화하듯 묻습니다.
            - 한 번에 하나의 질문만 합니다.

            출력은 다른 설명 없이 질문 문장 하나만 반환하세요. 따옴표나 접두사 없이 문장 자체만
            반환합니다.
            """;

    private static final String OPENER_SYSTEM_PROMPT_TEMPLATE = """
            당신은 AI 면접관입니다. 지금은 특정 항목(axis)에 캐물 지점 후보가 하나도 없어서,
            그 항목을 직접 여는 질문(seed question)을 새로 만들어야 합니다.

            아래 6대 평가 항목 정의를 참고하세요.
            %s

            아래는 항목별 여는 질문 예시입니다. 그대로 베끼지 말고 같은 결의 질문을 새로 만드세요.
            - ① 깊이: "최근 진행하신 작업 중에서, 왜 그렇게 만들었는지 원리까지 깊게 파고들어 보신 부분이 있다면 말씀해 주실 수 있을까요?"
            - ② 경계·규모: "그 작업을 하면서 트래픽이나 데이터 규모가 커졌을 때, 혹은 예상 못 한 예외 상황에서 한계에 부딪힌 경험이 있으셨나요?"
            - ③ 연결: "그 결정이 다른 팀이나 시스템, 혹은 지표에 어떤 영향을 줬는지 말씀해 주실 수 있을까요?"
            - ④ 대안·우선순위: "그 방법을 선택하기 전에 다른 대안도 고려해 보셨나요? 무엇을 먼저 두고 무엇을 포기하셨는지 궁금해요."
            - ⑤ 갈등: "기획 방향이 윗선이나 다른 팀과 부딪힌 경험이 있나요?"
            - ⑥ 성장·복원력: "일하면서 예상과 다르게 흘러갔거나 실패했던 경험, 그리고 거기서 무엇을 배우셨는지 말씀해 주실 수 있을까요?"

            규칙:
            - 기본은 입력으로 받는 지원자의 직무와 연차에 맞춘 일반적인 질문입니다.
              예를 들어 주니어에게는 팀이 이미 정해준 방향 안에서의 경험을, 시니어에게는
              스스로 의사결정하거나 남을 설득한 경험을 묻는 식으로 결을 맞춥니다.
            - [JD 키워드]와 [관련 포트폴리오 내용]이 함께 주어질 수 있습니다.
              그 포트폴리오 내용이 특정 JD 키워드를 실제로 뒷받침할 때만 그 키워드를 소재로
              자연스럽게 녹인 질문을 만드세요. 포트폴리오가 뒷받침하지 않는 JD 키워드는 절대 쓰지
              않고, 그럴 땐 그냥 직무·연차 기반 일반 질문으로 돌아갑니다. 이 둘이 주어지지 않으면
              무시합니다.
            - 지원자가 이미 한 말이나 채용 공고(JD) 문구를 그대로 인용하지 않습니다. 소재만
              참고하고 질문 문장은 새로 만듭니다.
            - 실제 면접관이 대화하듯 묻는 자연스러운 구어체 한 문장으로 답합니다.
            - 한 번에 하나의 질문만 합니다.

            출력은 다른 설명 없이 질문 문장 하나만 반환하세요. 따옴표나 접두사 없이 문장 자체만
            반환합니다.
            """;

    private final ChatClient chatClient;
    private final String openerSystemPrompt;

    AnthropicQuestionTextGeneratorAdapter(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.openerSystemPrompt = OPENER_SYSTEM_PROMPT_TEMPLATE.formatted(loadAxesYaml());
    }

    @Override
    public String generate(String probeText, String echoQuote) {
        String userMessage = """
                [캐물 의도] %s
                [되받아 물을 표현] %s
                """.formatted(probeText, echoQuote);

        try {
            String content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Anthropic이 빈 질문 문장을 반환했어요.");
            }
            return content.strip();
        } catch (Exception e) {
            log.error("[QUESTION TEXT GENERATE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("질문 문장 생성에 실패했어요.", e);
        }
    }

    @Override
    public String generateOpener(
            TestType axis, JobType jobType, Integer yearsOfExperience,
            List<String> jdKeywords, List<String> relatedPortfolioChunks
    ) {
        String userMessage = buildOpenerUserMessage(axis, jobType, yearsOfExperience, jdKeywords, relatedPortfolioChunks);

        try {
            String content = chatClient.prompt()
                    .system(openerSystemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Anthropic이 빈 여는 질문을 반환했어요.");
            }
            return content.strip();
        } catch (Exception e) {
            log.error("[QUESTION TEXT GENERATE] 여는 질문(opener) Anthropic 호출/파싱 실패: axis={}", axis, e);
            throw new RuntimeException("여는 질문 생성에 실패했어요.", e);
        }
    }

    private String buildOpenerUserMessage(
            TestType axis, JobType jobType, Integer yearsOfExperience,
            List<String> jdKeywords, List<String> relatedPortfolioChunks
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("[여는 질문을 만들 항목] ").append(axis.getLabel()).append("\n");
        sb.append("[지원자 직무] ").append(jobType.getLabel()).append("\n");
        sb.append("[지원자 연차] ").append(yearsOfExperience).append("년\n");
        if (jdKeywords != null && !jdKeywords.isEmpty()) {
            sb.append("[JD 키워드] ").append(String.join(", ", jdKeywords)).append("\n");
        }
        if (relatedPortfolioChunks != null && !relatedPortfolioChunks.isEmpty()) {
            sb.append("[관련 포트폴리오 내용]\n");
            for (String chunk : relatedPortfolioChunks) {
                sb.append("- ").append(chunk).append("\n");
            }
        }
        return sb.toString();
    }

    private static String loadAxesYaml() {
        try {
            return new ClassPathResource(AXES_YAML_PATH).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("axes.yaml 로드에 실패했어요.", e);
        }
    }
}

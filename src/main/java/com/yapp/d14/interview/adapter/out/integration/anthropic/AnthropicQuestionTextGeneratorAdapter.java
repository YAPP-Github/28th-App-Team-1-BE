package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.QuestionTextGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class AnthropicQuestionTextGeneratorAdapter implements QuestionTextGenerator {

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

    private final ChatClient chatClient;

    AnthropicQuestionTextGeneratorAdapter(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
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
            return content == null ? "" : content.strip();
        } catch (Exception e) {
            log.error("[QUESTION TEXT GENERATE] Anthropic 호출/파싱 실패", e);
            throw new RuntimeException("질문 문장 생성에 실패했어요.", e);
        }
    }
}

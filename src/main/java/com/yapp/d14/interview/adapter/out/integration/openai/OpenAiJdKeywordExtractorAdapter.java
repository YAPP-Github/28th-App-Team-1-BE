package com.yapp.d14.interview.adapter.out.integration.openai;

import com.yapp.d14.interview.application.command.AiUsageRecordCommand;
import com.yapp.d14.interview.application.port.in.AiUsageRecordUseCase;
import com.yapp.d14.interview.application.port.out.JdKeywordExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
class OpenAiJdKeywordExtractorAdapter implements JdKeywordExtractor {

    private static final String SYSTEM_PROMPT = """
            당신은 채용공고(JD)에서 핵심 기술·역량 키워드만 뽑는 역할입니다.
            지원자의 포트폴리오·질문 재료로 쓸 수 있는, 직무 관련성이 높은 키워드만 추립니다.
            절대 질문 문장이나 캐물지점을 만들지 마세요 - 키워드 리스트만 뽑습니다.
            복리후생·회사 소개·채용 절차 등 기술과 무관한 내용은 제외하세요.

            출력은 다른 설명 없이 JSON 배열 하나만 반환하세요. 예: ["대용량 트래픽", "MSA", "Kafka"]
            """;

    private final ChatClient chatClient;
    private final AiUsageRecordUseCase aiUsageRecordUseCase;

    OpenAiJdKeywordExtractorAdapter(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            AiUsageRecordUseCase aiUsageRecordUseCase
    ) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.aiUsageRecordUseCase = aiUsageRecordUseCase;
    }

    @Override
    public List<String> extractKeywords(Long sessionId, String jdText) {
        try {
            ResponseEntity<ChatResponse, List<String>> responseEntity = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(jdText)
                    .call()
                    .responseEntity(new ParameterizedTypeReference<List<String>>() {
                    });
            recordUsage(sessionId, responseEntity.response());
            List<String> keywords = responseEntity.entity();
            if (keywords == null) {
                throw new IllegalStateException("JD 키워드 추출 응답이 비어있어요.");
            }
            return keywords;
        } catch (Exception e) {
            log.error("[JD KEYWORD EXTRACT] OpenAI 호출/파싱 실패", e);
            throw new RuntimeException("JD 키워드 추출에 실패했어요.", e);
        }
    }

    private void recordUsage(Long sessionId, ChatResponse chatResponse) {
        if (sessionId == null || chatResponse == null || chatResponse.getMetadata() == null) {
            return;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        aiUsageRecordUseCase.record(AiUsageRecordCommand.openAiChat(
                sessionId,
                chatResponse.getMetadata().getModel(),
                usage.getPromptTokens() == null ? 0L : usage.getPromptTokens(),
                usage.getCompletionTokens() == null ? 0L : usage.getCompletionTokens()
        ));
    }
}

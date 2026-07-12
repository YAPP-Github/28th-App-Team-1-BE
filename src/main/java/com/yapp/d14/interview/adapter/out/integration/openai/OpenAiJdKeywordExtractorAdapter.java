package com.yapp.d14.interview.adapter.out.integration.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yapp.d14.interview.application.port.out.JdKeywordExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final ObjectMapper objectMapper;

    OpenAiJdKeywordExtractorAdapter(@Qualifier("openAiChatModel") ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> extractKeywords(String jdText) {
        String responseText;
        try {
            responseText = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(jdText)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[JD KEYWORD EXTRACT] OpenAI 호출 실패", e);
            throw new RuntimeException("JD 키워드 추출에 실패했어요.", e);
        }

        try {
            return objectMapper.readValue(responseText, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("[JD KEYWORD EXTRACT] 응답 파싱 실패: {}", responseText, e);
            throw new RuntimeException("JD 키워드 추출 응답 파싱에 실패했어요.", e);
        }
    }
}

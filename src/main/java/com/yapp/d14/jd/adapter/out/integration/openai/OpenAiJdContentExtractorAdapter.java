package com.yapp.d14.jd.adapter.out.integration.openai;

import com.yapp.d14.jd.application.port.out.JdContentExtractor;
import com.yapp.d14.jd.exception.JdExtractionFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
class OpenAiJdContentExtractorAdapter implements JdContentExtractor {

    private static final String SYSTEM_PROMPT = """
            당신은 채용공고 텍스트에서 JD(Job Description) 항목만 추출하는 전문가입니다.
            주어진 텍스트에서 다음 항목들만 추출하여 정리해주세요:
            - 직무 소개 / 담당 업무
            - 자격 요건 (필수)
            - 우대 사항
            - 기술 스택
            불필요한 회사 소개, 복리후생, 채용 절차 등은 제외하세요.
            항목별로 구분하여 명확하게 출력해주세요.
            """;

    private final ChatClient chatClient;

    OpenAiJdContentExtractorAdapter(@Qualifier("openAiChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String extract(String rawText) {
        String content;
        try {
            content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(rawText)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[JD EXTRACT] OpenAI 호출 실패", e);
            throw new JdExtractionFailedException("AI 처리 중 오류가 발생했습니다.", e);
        }

        if (!StringUtils.hasText(content)) {
            log.warn("[JD EXTRACT] OpenAI 응답이 비어있어요");
            throw new JdExtractionFailedException("AI가 JD 내용을 추출하지 못했어요.");
        }
        return content;
    }
}

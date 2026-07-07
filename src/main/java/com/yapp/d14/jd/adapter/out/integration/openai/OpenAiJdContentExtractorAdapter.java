package com.yapp.d14.jd.adapter.out.integration.openai;

import com.yapp.d14.common.properties.OpenAiProperties;
import com.yapp.d14.jd.application.port.out.JdContentExtractor;
import com.yapp.d14.jd.exception.JdExtractionFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class OpenAiJdContentExtractorAdapter implements JdContentExtractor {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final int TIMEOUT_MS = 30000;

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

    private final OpenAiProperties openAiProperties;
    private final RestClient restClient = RestClient.builder()
            .requestFactory(requestFactory())
            .build();

    @Override
    public String extract(String rawText) {
        OpenAiChatCompletionRequest request = new OpenAiChatCompletionRequest(
                openAiProperties.getModel(),
                openAiProperties.getMaxTokens(),
                openAiProperties.getTemperature(),
                List.of(
                        new OpenAiChatCompletionRequest.Message("system", SYSTEM_PROMPT),
                        new OpenAiChatCompletionRequest.Message("user", rawText)
                )
        );

        try {
            OpenAiChatCompletionResponse response = restClient.post()
                    .uri(CHAT_COMPLETIONS_URL)
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(OpenAiChatCompletionResponse.class);

            return response.choices().get(0).message().content();
        } catch (Exception e) {
            log.error("[JD EXTRACT] OpenAI 호출 실패", e);
            throw new JdExtractionFailedException("AI 처리 중 오류가 발생했습니다.", e);
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return factory;
    }
}

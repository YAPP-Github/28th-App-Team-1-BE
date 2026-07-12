package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.common.properties.AnthropicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class AnthropicClient {

    private static final String MESSAGES_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int TIMEOUT_MS = 30000;

    private final AnthropicProperties anthropicProperties;
    private final RestClient restClient = RestClient.builder()
            .requestFactory(requestFactory())
            .build();

    String complete(String systemPrompt, String userMessage) {
        AnthropicMessageRequest request = new AnthropicMessageRequest(
                anthropicProperties.getModel(),
                anthropicProperties.getMaxTokens(),
                anthropicProperties.getTemperature(),
                systemPrompt,
                List.of(new AnthropicMessageRequest.Message("user", userMessage))
        );

        try {
            AnthropicMessageResponse response = restClient.post()
                    .uri(MESSAGES_URL)
                    .header("x-api-key", anthropicProperties.getApiKey())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .body(request)
                    .retrieve()
                    .body(AnthropicMessageResponse.class);

            return response.content().get(0).text();
        } catch (Exception e) {
            log.error("[ANTHROPIC] 호출 실패", e);
            throw new RuntimeException("Anthropic 호출에 실패했어요.", e);
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return factory;
    }
}

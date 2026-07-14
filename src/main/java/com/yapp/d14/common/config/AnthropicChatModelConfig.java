package com.yapp.d14.common.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatProperties;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

/**
 * Spring AI의 Anthropic 자동 구성은 {@code temperature} 기본값(0.8)을 항상 채워 보내는데,
 * claude-sonnet-5는 해당 파라미터를 더 이상 허용하지 않아 400 오류가 발생한다.
 * 또한 이 모델은 기본적으로 extended thinking을 사용해 max_tokens 예산을 thinking에
 * 모두 소진하고 실제 답변 텍스트를 한 글자도 못 내놓는 경우가 있어(응답 파싱 실패로 이어짐)
 * 단순 추출용 호출에서는 thinking을 명시적으로 꺼둔다.
 * temperature를 빼고 thinking을 비활성화한 옵션으로 AnthropicChatModel을 직접 구성해
 * 자동 구성을 대체한다.
 */
@Configuration
class AnthropicChatModelConfig {

    @Bean
    AnthropicChatModel anthropicChatModel(
            AnthropicApi anthropicApi,
            AnthropicChatProperties chatProperties,
            RetryTemplate retryTemplate,
            ToolCallingManager toolCallingManager,
            ObjectProvider<ObservationRegistry> observationRegistry) {

        AnthropicChatOptions defaultOptions = AnthropicChatOptions.builder()
                .model(chatProperties.getOptions().getModel())
                .maxTokens(chatProperties.getOptions().getMaxTokens())
                .thinking(AnthropicApi.ThinkingType.DISABLED, null)
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(defaultOptions)
                .retryTemplate(retryTemplate)
                .toolCallingManager(toolCallingManager)
                .observationRegistry(observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP))
                .build();
    }
}

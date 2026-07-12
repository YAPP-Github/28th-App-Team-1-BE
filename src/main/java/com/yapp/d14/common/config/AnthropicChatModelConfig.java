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
 * temperature를 뺀 옵션으로 AnthropicChatModel을 직접 구성해 자동 구성을 대체한다.
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

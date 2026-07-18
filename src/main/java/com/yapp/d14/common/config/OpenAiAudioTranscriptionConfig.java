package com.yapp.d14.common.config;

import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI의 {@code OpenAiAudioTranscriptionModel}은 Whisper verbose_json 응답의 세그먼트(no_speech_prob 등)를
 * 버리고 텍스트만 남긴다 — 이 감싸는 {@code AudioTranscription}의 생성자가 String 하나만 받기 때문이다.
 * STT 실패율 계산에는 세그먼트별 no_speech_prob이 필요하므로, 오토컨피그가 빈으로 노출하지 않는
 * 저수준 {@link OpenAiAudioApi}를 직접 구성해 사용한다.
 */
@Configuration
class OpenAiAudioTranscriptionConfig {

    @Bean
    OpenAiAudioApi openAiAudioApi(OpenAiConnectionProperties connectionProperties) {
        return OpenAiAudioApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .baseUrl(connectionProperties.getBaseUrl())
                .build();
    }
}

package com.yapp.d14.common.config;

import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Redis 명령 지연·호출량(D-3)은 Spring Boot가 자동 계측하지 않아 직접 물려준다.
@Configuration
public class LettuceMetricsConfig {

    @Bean
    public ClientResources lettuceClientResources(MeterRegistry meterRegistry) {
        return ClientResources.builder()
                .commandLatencyRecorder(
                        new MicrometerCommandLatencyRecorder(meterRegistry, MicrometerOptions.create()))
                .build();
    }
}

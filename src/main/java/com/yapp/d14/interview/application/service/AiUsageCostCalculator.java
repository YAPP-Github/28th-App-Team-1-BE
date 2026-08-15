package com.yapp.d14.interview.application.service;

import com.yapp.d14.common.properties.AiPricingProperties;
import com.yapp.d14.interview.application.command.AiUsageRecordCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
class AiUsageCostCalculator {

    private static final long MILLIS_PER_SECOND = 1_000L;

    private final AiPricingProperties aiPricingProperties;
    private final Set<String> warnedModels = ConcurrentHashMap.newKeySet();

    long calculateNanoUsd(AiUsageRecordCommand command) {
        AiPricingProperties.ModelPrice price = aiPricingProperties.priceOf(command.model());
        if (price == null) {
            if (warnedModels.add(String.valueOf(command.model()))) {
                log.warn("[AI USAGE] 단가가 등록되지 않은 모델이라 비용을 0으로 기록해요 - model={}, provider={}",
                        command.model(), command.provider());
            }
            return 0L;
        }
        return command.inputTokens() * price.getInputNanoUsdPerToken()
                + command.outputTokens() * price.getOutputNanoUsdPerToken()
                + command.cacheWriteTokens() * price.getCacheWriteNanoUsdPerToken()
                + command.cacheReadTokens() * price.getCacheReadNanoUsdPerToken()
                + command.ttsCharacters() * price.getNanoUsdPerCharacter()
                + command.sttDurationMillis() * price.getNanoUsdPerSecond() / MILLIS_PER_SECOND;
    }
}

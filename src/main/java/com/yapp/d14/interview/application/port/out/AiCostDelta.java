package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.AiProvider;

public record AiCostDelta(
        Long sessionId,
        AiProvider provider,
        long inputTokens,
        long outputTokens,
        long cacheWriteTokens,
        long cacheReadTokens,
        long ttsCharacters,
        long sttDurationMillis,
        long costNanoUsd
) {
}

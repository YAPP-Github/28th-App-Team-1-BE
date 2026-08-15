package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;

@Getter
public class InterviewSessionAiCost {

    private final Long sessionId;

    private final long anthropicInputTokens;
    private final long anthropicOutputTokens;
    private final long anthropicCacheWriteTokens;
    private final long anthropicCacheReadTokens;
    private final long anthropicCostNanoUsd;

    private final long openAiInputTokens;
    private final long openAiOutputTokens;
    private final long openAiTtsCharacters;
    private final long openAiSttDurationMillis;
    private final long openAiCostNanoUsd;

    private final long totalCostNanoUsd;
    private final int callCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewSessionAiCost(
            Long sessionId,
            long anthropicInputTokens,
            long anthropicOutputTokens,
            long anthropicCacheWriteTokens,
            long anthropicCacheReadTokens,
            long anthropicCostNanoUsd,
            long openAiInputTokens,
            long openAiOutputTokens,
            long openAiTtsCharacters,
            long openAiSttDurationMillis,
            long openAiCostNanoUsd,
            long totalCostNanoUsd,
            int callCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.sessionId = sessionId;
        this.anthropicInputTokens = anthropicInputTokens;
        this.anthropicOutputTokens = anthropicOutputTokens;
        this.anthropicCacheWriteTokens = anthropicCacheWriteTokens;
        this.anthropicCacheReadTokens = anthropicCacheReadTokens;
        this.anthropicCostNanoUsd = anthropicCostNanoUsd;
        this.openAiInputTokens = openAiInputTokens;
        this.openAiOutputTokens = openAiOutputTokens;
        this.openAiTtsCharacters = openAiTtsCharacters;
        this.openAiSttDurationMillis = openAiSttDurationMillis;
        this.openAiCostNanoUsd = openAiCostNanoUsd;
        this.totalCostNanoUsd = totalCostNanoUsd;
        this.callCount = callCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InterviewSessionAiCost of(
            Long sessionId,
            long anthropicInputTokens,
            long anthropicOutputTokens,
            long anthropicCacheWriteTokens,
            long anthropicCacheReadTokens,
            long anthropicCostNanoUsd,
            long openAiInputTokens,
            long openAiOutputTokens,
            long openAiTtsCharacters,
            long openAiSttDurationMillis,
            long openAiCostNanoUsd,
            long totalCostNanoUsd,
            int callCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return InterviewSessionAiCost.builder()
                .sessionId(sessionId)
                .anthropicInputTokens(anthropicInputTokens)
                .anthropicOutputTokens(anthropicOutputTokens)
                .anthropicCacheWriteTokens(anthropicCacheWriteTokens)
                .anthropicCacheReadTokens(anthropicCacheReadTokens)
                .anthropicCostNanoUsd(anthropicCostNanoUsd)
                .openAiInputTokens(openAiInputTokens)
                .openAiOutputTokens(openAiOutputTokens)
                .openAiTtsCharacters(openAiTtsCharacters)
                .openAiSttDurationMillis(openAiSttDurationMillis)
                .openAiCostNanoUsd(openAiCostNanoUsd)
                .totalCostNanoUsd(totalCostNanoUsd)
                .callCount(callCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private static final BigDecimal NANO_PER_USD = BigDecimal.valueOf(1_000_000_000L);

    public BigDecimal totalCostUsd() {
        return BigDecimal.valueOf(totalCostNanoUsd).divide(NANO_PER_USD, MathContext.DECIMAL64);
    }

    public BigDecimal anthropicCostUsd() {
        return BigDecimal.valueOf(anthropicCostNanoUsd).divide(NANO_PER_USD, MathContext.DECIMAL64);
    }

    public BigDecimal openAiCostUsd() {
        return BigDecimal.valueOf(openAiCostNanoUsd).divide(NANO_PER_USD, MathContext.DECIMAL64);
    }
}

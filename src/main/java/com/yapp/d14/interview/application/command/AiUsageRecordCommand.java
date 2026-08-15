package com.yapp.d14.interview.application.command;

import com.yapp.d14.interview.domain.AiProvider;

public record AiUsageRecordCommand(
        Long sessionId,
        AiProvider provider,
        String model,
        long inputTokens,
        long outputTokens,
        long cacheWriteTokens,
        long cacheReadTokens,
        long ttsCharacters,
        long sttDurationMillis
) {

    public static AiUsageRecordCommand anthropicChat(
            Long sessionId, String model, long inputTokens, long outputTokens, long cacheWriteTokens, long cacheReadTokens
    ) {
        return new AiUsageRecordCommand(
                sessionId, AiProvider.ANTHROPIC, model, inputTokens, outputTokens, cacheWriteTokens, cacheReadTokens, 0L, 0L);
    }

    public static AiUsageRecordCommand openAiChat(Long sessionId, String model, long inputTokens, long outputTokens) {
        return new AiUsageRecordCommand(sessionId, AiProvider.OPENAI, model, inputTokens, outputTokens, 0L, 0L, 0L, 0L);
    }

    public static AiUsageRecordCommand openAiEmbedding(Long sessionId, String model, long inputTokens) {
        return new AiUsageRecordCommand(sessionId, AiProvider.OPENAI, model, inputTokens, 0L, 0L, 0L, 0L, 0L);
    }

    public static AiUsageRecordCommand openAiTts(Long sessionId, String model, long characters) {
        return new AiUsageRecordCommand(sessionId, AiProvider.OPENAI, model, 0L, 0L, 0L, 0L, characters, 0L);
    }

    public static AiUsageRecordCommand openAiStt(Long sessionId, String model, long durationMillis) {
        return new AiUsageRecordCommand(sessionId, AiProvider.OPENAI, model, 0L, 0L, 0L, 0L, 0L, durationMillis);
    }
}

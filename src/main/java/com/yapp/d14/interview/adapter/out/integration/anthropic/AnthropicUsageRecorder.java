package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.command.AiUsageRecordCommand;
import com.yapp.d14.interview.application.port.in.AiUsageRecordUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AnthropicUsageRecorder {

    private final AiUsageRecordUseCase aiUsageRecordUseCase;

    void record(Long sessionId, ChatResponse chatResponse) {
        if (sessionId == null || chatResponse == null || chatResponse.getMetadata() == null) {
            return;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }
        long cacheWriteTokens = 0L;
        long cacheReadTokens = 0L;
        if (usage.getNativeUsage() instanceof AnthropicApi.Usage nativeUsage) {
            cacheWriteTokens = toLong(nativeUsage.cacheCreationInputTokens());
            cacheReadTokens = toLong(nativeUsage.cacheReadInputTokens());
        }

        aiUsageRecordUseCase.record(AiUsageRecordCommand.anthropicChat(
                sessionId,
                chatResponse.getMetadata().getModel(),
                toLong(usage.getPromptTokens()),
                toLong(usage.getCompletionTokens()),
                cacheWriteTokens,
                cacheReadTokens
        ));
    }

    String recordAndText(Long sessionId, ChatResponse chatResponse) {
        record(sessionId, chatResponse);
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return null;
        }
        return chatResponse.getResult().getOutput().getText();
    }

    private static long toLong(Integer value) {
        return value == null ? 0L : value;
    }
}

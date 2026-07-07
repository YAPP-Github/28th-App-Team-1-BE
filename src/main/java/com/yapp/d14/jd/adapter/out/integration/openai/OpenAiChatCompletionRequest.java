package com.yapp.d14.jd.adapter.out.integration.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record OpenAiChatCompletionRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        double temperature,
        List<Message> messages
) {

    record Message(String role, String content) {
    }
}

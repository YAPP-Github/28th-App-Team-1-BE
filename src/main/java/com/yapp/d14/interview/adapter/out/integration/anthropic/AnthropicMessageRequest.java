package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record AnthropicMessageRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        double temperature,
        String system,
        List<Message> messages
) {

    record Message(String role, String content) {
    }
}

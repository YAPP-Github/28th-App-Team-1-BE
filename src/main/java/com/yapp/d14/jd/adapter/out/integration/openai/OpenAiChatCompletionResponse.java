package com.yapp.d14.jd.adapter.out.integration.openai;

import java.util.List;

record OpenAiChatCompletionResponse(List<Choice> choices) {

    record Choice(Message message) {
    }

    record Message(String role, String content) {
    }
}

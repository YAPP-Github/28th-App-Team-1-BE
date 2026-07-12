package com.yapp.d14.interview.adapter.out.integration.anthropic;

import java.util.List;

record AnthropicMessageResponse(List<Content> content) {

    record Content(String type, String text) {
    }
}

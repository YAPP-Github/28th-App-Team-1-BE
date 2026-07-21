package com.yapp.d14.interview.domain;

import java.util.List;

public record HighlightSpan(TextRange range, HighlightTone tone, List<ActionKeyword> actionKeywords) {

    private static final int MAX_ACTION_KEYWORDS = 3;

    public HighlightSpan {
        if (actionKeywords != null && actionKeywords.size() > MAX_ACTION_KEYWORDS) {
            throw new IllegalArgumentException("행동형 키워드는 하이라이트당 최대 %d개입니다.".formatted(MAX_ACTION_KEYWORDS));
        }
    }
}

package com.yapp.d14.interview.domain;

import java.util.List;

// followUpQuestions: 이 하이라이트 구간에 대해 면접관이 이어서 던질 법한 추가 질문(0~3개).
public record HighlightSpan(TextRange range, HighlightTone tone, String analysis, List<String> followUpQuestions) {
}

package com.yapp.d14.interview.domain;

public record ActionKeyword(
        String keyword,
        String problemAnalysis,
        String improvementReason,
        String applicationMethod,
        int priority
) {
}

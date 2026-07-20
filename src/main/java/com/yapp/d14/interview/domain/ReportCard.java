package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ReportCard {

    private static final int MAX_ACTION_KEYWORDS = 3;

    private final Long id;
    private final Long sessionId;
    private final Long questionId;
    private final int depthLevel;
    private final TestType testType;
    private final String questionIntentTranslation;
    private final List<HighlightSpan> highlightSpans;
    private final List<ActionKeyword> actionKeywords;
    private final RewriteSuggestion rewriteSuggestion;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ReportCard(
            Long id,
            Long sessionId,
            Long questionId,
            int depthLevel,
            TestType testType,
            String questionIntentTranslation,
            List<HighlightSpan> highlightSpans,
            List<ActionKeyword> actionKeywords,
            RewriteSuggestion rewriteSuggestion,
            LocalDateTime createdAt
    ) {
        if (actionKeywords != null && actionKeywords.size() > MAX_ACTION_KEYWORDS) {
            throw new IllegalArgumentException("행동형 키워드는 카드당 최대 %d개입니다.".formatted(MAX_ACTION_KEYWORDS));
        }
        this.id = id;
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.depthLevel = depthLevel;
        this.testType = testType;
        this.questionIntentTranslation = questionIntentTranslation;
        this.highlightSpans = highlightSpans;
        this.actionKeywords = actionKeywords;
        this.rewriteSuggestion = rewriteSuggestion;
        this.createdAt = createdAt;
    }

    public static ReportCard create(
            Long sessionId,
            Long questionId,
            int depthLevel,
            TestType testType,
            String questionIntentTranslation,
            List<HighlightSpan> highlightSpans,
            List<ActionKeyword> actionKeywords,
            RewriteSuggestion rewriteSuggestion
    ) {
        return ReportCard.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .depthLevel(depthLevel)
                .testType(testType)
                .questionIntentTranslation(questionIntentTranslation)
                .highlightSpans(highlightSpans)
                .actionKeywords(actionKeywords)
                .rewriteSuggestion(rewriteSuggestion)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ReportCard of(
            Long id,
            Long sessionId,
            Long questionId,
            int depthLevel,
            TestType testType,
            String questionIntentTranslation,
            List<HighlightSpan> highlightSpans,
            List<ActionKeyword> actionKeywords,
            RewriteSuggestion rewriteSuggestion,
            LocalDateTime createdAt
    ) {
        return ReportCard.builder()
                .id(id)
                .sessionId(sessionId)
                .questionId(questionId)
                .depthLevel(depthLevel)
                .testType(testType)
                .questionIntentTranslation(questionIntentTranslation)
                .highlightSpans(highlightSpans)
                .actionKeywords(actionKeywords)
                .rewriteSuggestion(rewriteSuggestion)
                .createdAt(createdAt)
                .build();
    }
}

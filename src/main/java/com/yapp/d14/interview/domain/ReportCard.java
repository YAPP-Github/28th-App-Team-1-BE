package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ReportCard {

    private final Long id;
    private final Long sessionId;
    private final Long questionId;
    private final int depthLevel;
    private final TestType testType;
    private final String questionIntentTranslation;
    private final List<HighlightSpan> highlightSpans;
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
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.depthLevel = depthLevel;
        this.testType = testType;
        this.questionIntentTranslation = questionIntentTranslation;
        this.highlightSpans = highlightSpans;
        this.createdAt = createdAt;
    }

    public static ReportCard create(
            Long sessionId,
            Long questionId,
            int depthLevel,
            TestType testType,
            String questionIntentTranslation,
            List<HighlightSpan> highlightSpans
    ) {
        return ReportCard.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .depthLevel(depthLevel)
                .testType(testType)
                .questionIntentTranslation(questionIntentTranslation)
                .highlightSpans(highlightSpans)
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
                .createdAt(createdAt)
                .build();
    }
}

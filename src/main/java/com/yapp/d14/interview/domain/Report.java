package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Report {

    private final Long id;
    private final Long sessionId;
    private final Double compositeScore;
    private final InternalGrade internalGrade;
    private final String headline;
    private final HeadlineBranch headlineBranch;
    private final ReportStatus status;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Report(
            Long id,
            Long sessionId,
            Double compositeScore,
            InternalGrade internalGrade,
            String headline,
            HeadlineBranch headlineBranch,
            ReportStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.compositeScore = compositeScore;
        this.internalGrade = internalGrade;
        this.headline = headline;
        this.headlineBranch = headlineBranch;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Report create(
            Long sessionId,
            Double compositeScore,
            InternalGrade internalGrade,
            String headline,
            HeadlineBranch headlineBranch,
            ReportStatus status
    ) {
        return Report.builder()
                .sessionId(sessionId)
                .compositeScore(compositeScore)
                .internalGrade(internalGrade)
                .headline(headline)
                .headlineBranch(headlineBranch)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Report of(
            Long id,
            Long sessionId,
            Double compositeScore,
            InternalGrade internalGrade,
            String headline,
            HeadlineBranch headlineBranch,
            ReportStatus status,
            LocalDateTime createdAt
    ) {
        return Report.builder()
                .id(id)
                .sessionId(sessionId)
                .compositeScore(compositeScore)
                .internalGrade(internalGrade)
                .headline(headline)
                .headlineBranch(headlineBranch)
                .status(status)
                .createdAt(createdAt)
                .build();
    }
}

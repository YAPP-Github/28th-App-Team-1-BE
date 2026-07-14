package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AxisEvaluation {

    private final Long id;
    private final Long sessionId;
    private final TestType testType;
    private final Integer score;
    private final Integer appliedCap;
    private final ResolutionLevel resolutionLevel;
    private final ResolutionLowReason resolutionLowReason;
    private final List<TimeRange> evidenceTimestamps;
    private final String rationale;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private AxisEvaluation(
            Long id,
            Long sessionId,
            TestType testType,
            Integer score,
            Integer appliedCap,
            ResolutionLevel resolutionLevel,
            ResolutionLowReason resolutionLowReason,
            List<TimeRange> evidenceTimestamps,
            String rationale,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.testType = testType;
        this.score = score;
        this.appliedCap = appliedCap;
        this.resolutionLevel = resolutionLevel;
        this.resolutionLowReason = resolutionLowReason;
        this.evidenceTimestamps = evidenceTimestamps;
        this.rationale = rationale;
        this.createdAt = createdAt;
    }

    public static AxisEvaluation create(
            Long sessionId,
            TestType testType,
            Integer score,
            ResolutionLevel resolutionLevel,
            ResolutionLowReason resolutionLowReason,
            List<TimeRange> evidenceTimestamps,
            String rationale
    ) {
        return AxisEvaluation.builder()
                .sessionId(sessionId)
                .testType(testType)
                .score(score)
                .resolutionLevel(resolutionLevel)
                .resolutionLowReason(resolutionLowReason)
                .evidenceTimestamps(evidenceTimestamps)
                .rationale(rationale)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AxisEvaluation of(
            Long id,
            Long sessionId,
            TestType testType,
            Integer score,
            Integer appliedCap,
            ResolutionLevel resolutionLevel,
            ResolutionLowReason resolutionLowReason,
            List<TimeRange> evidenceTimestamps,
            String rationale,
            LocalDateTime createdAt
    ) {
        return AxisEvaluation.builder()
                .id(id)
                .sessionId(sessionId)
                .testType(testType)
                .score(score)
                .appliedCap(appliedCap)
                .resolutionLevel(resolutionLevel)
                .resolutionLowReason(resolutionLowReason)
                .evidenceTimestamps(evidenceTimestamps)
                .rationale(rationale)
                .createdAt(createdAt)
                .build();
    }

    public AxisEvaluation withAppliedCap(Integer cap) {
        return AxisEvaluation.of(
                id, sessionId, testType, score, cap,
                resolutionLevel, resolutionLowReason, evidenceTimestamps, rationale, createdAt
        );
    }

    public int effectiveScore() {
        return appliedCap == null ? score : Math.min(score, appliedCap);
    }
}

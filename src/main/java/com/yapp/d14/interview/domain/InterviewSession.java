package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
public class InterviewSession {

    private final Long id;
    private final UUID userId;
    private final UUID portfolioId;
    private final JobType snapshotJobType;
    private final Integer snapshotYearsOfExperience;
    private final String jdUrl;
    private final String jdText;
    private final String focusProject;
    private InterviewSessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private InterviewEndType endType;
    private Integer weightDepth;
    private Integer weightBoundary;
    private Integer weightConnection;
    private Integer weightTradeoff;
    private Integer weightConflict;
    private Integer weightResilience;

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewSession(
            Long id,
            UUID userId,
            UUID portfolioId,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject,
            InterviewSessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            InterviewEndType endType,
            Integer weightDepth,
            Integer weightBoundary,
            Integer weightConnection,
            Integer weightTradeoff,
            Integer weightConflict,
            Integer weightResilience
    ) {
        this.id = id;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.snapshotJobType = snapshotJobType;
        this.snapshotYearsOfExperience = snapshotYearsOfExperience;
        this.jdUrl = jdUrl;
        this.jdText = jdText;
        this.focusProject = focusProject;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.endType = endType;
        this.weightDepth = weightDepth;
        this.weightBoundary = weightBoundary;
        this.weightConnection = weightConnection;
        this.weightTradeoff = weightTradeoff;
        this.weightConflict = weightConflict;
        this.weightResilience = weightResilience;
    }

    public static InterviewSession create(
            UUID userId,
            UUID portfolioId,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject
    ) {
        return InterviewSession.builder()
                .userId(userId)
                .portfolioId(portfolioId)
                .snapshotJobType(snapshotJobType)
                .snapshotYearsOfExperience(snapshotYearsOfExperience)
                .jdUrl(jdUrl)
                .jdText(jdText)
                .focusProject(focusProject)
                .status(InterviewSessionStatus.PREPARING)
                .build();
    }

    public void markReady() {
        this.status = InterviewSessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void markPreloadFailed() {
        this.status = InterviewSessionStatus.PRELOAD_FAILED;
    }

    public void markCompleted(InterviewEndType endType) {
        this.status = InterviewSessionStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
        this.endType = endType;
    }

    // FirstCoreAxisSelector 등 axis 우선순위 판단 로직에 넘길 때 쓰는 조회용 헬퍼
    public Map<TestType, Integer> getWeights() {
        return Map.of(
                TestType.DEPTH, weightDepth,
                TestType.BOUNDARY, weightBoundary,
                TestType.CONNECTION, weightConnection,
                TestType.TRADEOFF, weightTradeoff,
                TestType.CONFLICT, weightConflict,
                TestType.RESILIENCE, weightResilience
        );
    }

    public void assignWeights(Map<TestType, Integer> weights) {
        this.weightDepth = weights.get(TestType.DEPTH);
        this.weightBoundary = weights.get(TestType.BOUNDARY);
        this.weightConnection = weights.get(TestType.CONNECTION);
        this.weightTradeoff = weights.get(TestType.TRADEOFF);
        this.weightConflict = weights.get(TestType.CONFLICT);
        this.weightResilience = weights.get(TestType.RESILIENCE);
    }

    public static InterviewSession of(
            Long id,
            UUID userId,
            UUID portfolioId,
            JobType snapshotJobType,
            Integer snapshotYearsOfExperience,
            String jdUrl,
            String jdText,
            String focusProject,
            InterviewSessionStatus status,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            InterviewEndType endType,
            Integer weightDepth,
            Integer weightBoundary,
            Integer weightConnection,
            Integer weightTradeoff,
            Integer weightConflict,
            Integer weightResilience
    ) {
        return InterviewSession.builder()
                .id(id)
                .userId(userId)
                .portfolioId(portfolioId)
                .snapshotJobType(snapshotJobType)
                .snapshotYearsOfExperience(snapshotYearsOfExperience)
                .jdUrl(jdUrl)
                .jdText(jdText)
                .focusProject(focusProject)
                .status(status)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .endType(endType)
                .weightDepth(weightDepth)
                .weightBoundary(weightBoundary)
                .weightConnection(weightConnection)
                .weightTradeoff(weightTradeoff)
                .weightConflict(weightConflict)
                .weightResilience(weightResilience)
                .build();
    }
}

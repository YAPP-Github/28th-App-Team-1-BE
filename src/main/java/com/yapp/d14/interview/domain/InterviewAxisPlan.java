package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InterviewAxisPlan {

    private final Long id;
    private final Long sessionId;
    private final TestType testType;
    private final AxisTier tier;
    private final Integer budget;
    private Integer usedCount;
    private boolean completed;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewAxisPlan(
            Long id,
            Long sessionId,
            TestType testType,
            AxisTier tier,
            Integer budget,
            Integer usedCount,
            boolean completed,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.testType = testType;
        this.tier = tier;
        this.budget = budget;
        this.usedCount = usedCount;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public static InterviewAxisPlan create(
            Long sessionId,
            TestType testType,
            AxisTier tier,
            Integer budget
    ) {
        return InterviewAxisPlan.builder()
                .sessionId(sessionId)
                .testType(testType)
                .tier(tier)
                .budget(budget)
                .usedCount(0)
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void incrementUsedCount() {
        this.usedCount = this.usedCount + 1;
    }

    public void markCompleted() {
        this.completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public static InterviewAxisPlan of(
            Long id,
            Long sessionId,
            TestType testType,
            AxisTier tier,
            Integer budget,
            Integer usedCount,
            boolean completed,
            LocalDateTime createdAt
    ) {
        return InterviewAxisPlan.builder()
                .id(id)
                .sessionId(sessionId)
                .testType(testType)
                .tier(tier)
                .budget(budget)
                .usedCount(usedCount)
                .completed(completed)
                .createdAt(createdAt)
                .build();
    }
}

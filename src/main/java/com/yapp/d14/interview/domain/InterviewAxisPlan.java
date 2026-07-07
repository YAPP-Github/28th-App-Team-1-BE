package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class InterviewAxisPlan {

    private final Long id;
    private final Long interviewSessionId;
    private final TestType axis;
    private final AxisTier tier;
    private final Integer budget;
    private Integer usedCount;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private InterviewAxisPlan(
            Long id,
            Long interviewSessionId,
            TestType axis,
            AxisTier tier,
            Integer budget,
            Integer usedCount,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.interviewSessionId = interviewSessionId;
        this.axis = axis;
        this.tier = tier;
        this.budget = budget;
        this.usedCount = usedCount;
        this.createdAt = createdAt;
    }

    public static InterviewAxisPlan create(
            Long interviewSessionId,
            TestType axis,
            AxisTier tier,
            Integer budget
    ) {
        return InterviewAxisPlan.builder()
                .interviewSessionId(interviewSessionId)
                .axis(axis)
                .tier(tier)
                .budget(budget)
                .usedCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static InterviewAxisPlan of(
            Long id,
            Long interviewSessionId,
            TestType axis,
            AxisTier tier,
            Integer budget,
            Integer usedCount,
            LocalDateTime createdAt
    ) {
        return InterviewAxisPlan.builder()
                .id(id)
                .interviewSessionId(interviewSessionId)
                .axis(axis)
                .tier(tier)
                .budget(budget)
                .usedCount(usedCount)
                .createdAt(createdAt)
                .build();
    }
}

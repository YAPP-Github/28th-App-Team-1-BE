package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.AxisTier;
import com.yapp.d14.interview.domain.InterviewAxisPlan;
import com.yapp.d14.interview.domain.TestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_axis_plan", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "test_type"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewAxisPlanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type")
    private TestType testType;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    private AxisTier tier;

    @Column(name = "budget")
    private Integer budget;

    @Column(name = "used_count")
    private Integer usedCount;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static InterviewAxisPlanJpaEntity from(InterviewAxisPlan interviewAxisPlan) {
        InterviewAxisPlanJpaEntity entity = new InterviewAxisPlanJpaEntity();
        entity.id = interviewAxisPlan.getId();
        entity.sessionId = interviewAxisPlan.getSessionId();
        entity.testType = interviewAxisPlan.getTestType();
        entity.tier = interviewAxisPlan.getTier();
        entity.budget = interviewAxisPlan.getBudget();
        entity.usedCount = interviewAxisPlan.getUsedCount();
        entity.completed = interviewAxisPlan.isCompleted();
        entity.createdAt = interviewAxisPlan.getCreatedAt();
        return entity;
    }

    public InterviewAxisPlan toDomain() {
        return InterviewAxisPlan.of(
                id,
                sessionId,
                testType,
                tier,
                budget,
                usedCount,
                completed,
                createdAt
        );
    }
}

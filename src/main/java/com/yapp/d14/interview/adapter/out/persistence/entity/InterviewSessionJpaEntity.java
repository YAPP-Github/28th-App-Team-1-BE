package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.InterviewEndType;
import com.yapp.d14.interview.domain.InterviewSession;
import com.yapp.d14.interview.domain.InterviewSessionStatus;
import com.yapp.d14.interview.domain.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "portfolio_id", columnDefinition = "uuid")
    private UUID portfolioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_job_type")
    private JobType snapshotJobType;

    @Column(name = "snapshot_years_of_experience")
    private Integer snapshotYearsOfExperience;

    @Column(name = "jd_url")
    private String jdUrl;

    @Column(name = "jd_text", columnDefinition = "TEXT")
    private String jdText;

    @Column(name = "focus_project")
    private String focusProject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewSessionStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_type", length = 20)
    private InterviewEndType endType;

    @Column(name = "weight_depth")
    private Integer weightDepth;

    @Column(name = "weight_boundary")
    private Integer weightBoundary;

    @Column(name = "weight_connection")
    private Integer weightConnection;

    @Column(name = "weight_tradeoff")
    private Integer weightTradeoff;

    @Column(name = "weight_conflict")
    private Integer weightConflict;

    @Column(name = "weight_resilience")
    private Integer weightResilience;

    public static InterviewSessionJpaEntity from(InterviewSession interviewSession) {
        InterviewSessionJpaEntity entity = new InterviewSessionJpaEntity();
        entity.id = interviewSession.getId();
        entity.userId = interviewSession.getUserId();
        entity.portfolioId = interviewSession.getPortfolioId();
        entity.snapshotJobType = interviewSession.getSnapshotJobType();
        entity.snapshotYearsOfExperience = interviewSession.getSnapshotYearsOfExperience();
        entity.jdUrl = interviewSession.getJdUrl();
        entity.jdText = interviewSession.getJdText();
        entity.focusProject = interviewSession.getFocusProject();
        entity.status = interviewSession.getStatus();
        entity.startedAt = interviewSession.getStartedAt();
        entity.endedAt = interviewSession.getEndedAt();
        entity.endType = interviewSession.getEndType();
        entity.weightDepth = interviewSession.getWeightDepth();
        entity.weightBoundary = interviewSession.getWeightBoundary();
        entity.weightConnection = interviewSession.getWeightConnection();
        entity.weightTradeoff = interviewSession.getWeightTradeoff();
        entity.weightConflict = interviewSession.getWeightConflict();
        entity.weightResilience = interviewSession.getWeightResilience();
        return entity;
    }

    public InterviewSession toDomain() {
        return InterviewSession.of(
                id,
                userId,
                portfolioId,
                snapshotJobType,
                snapshotYearsOfExperience,
                jdUrl,
                jdText,
                focusProject,
                status,
                startedAt,
                endedAt,
                endType,
                weightDepth,
                weightBoundary,
                weightConnection,
                weightTradeoff,
                weightConflict,
                weightResilience
        );
    }
}

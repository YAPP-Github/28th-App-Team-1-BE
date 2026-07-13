package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.HeadlineBranch;
import com.yapp.d14.interview.domain.InternalGrade;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportStatus;
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

@Entity
@Table(name = "report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "composite_score")
    private Double compositeScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "internal_grade")
    private InternalGrade internalGrade;

    @Column(name = "headline", columnDefinition = "TEXT")
    private String headline;

    @Enumerated(EnumType.STRING)
    @Column(name = "headline_branch")
    private HeadlineBranch headlineBranch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static ReportJpaEntity from(Report report) {
        ReportJpaEntity entity = new ReportJpaEntity();
        entity.id = report.getId();
        entity.sessionId = report.getSessionId();
        entity.compositeScore = report.getCompositeScore();
        entity.internalGrade = report.getInternalGrade();
        entity.headline = report.getHeadline();
        entity.headlineBranch = report.getHeadlineBranch();
        entity.status = report.getStatus();
        entity.createdAt = report.getCreatedAt();
        return entity;
    }

    public Report toDomain() {
        return Report.of(
                id,
                sessionId,
                compositeScore,
                internalGrade,
                headline,
                headlineBranch,
                status,
                createdAt
        );
    }
}

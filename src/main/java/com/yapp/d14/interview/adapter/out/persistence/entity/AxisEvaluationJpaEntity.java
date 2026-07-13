package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.ResolutionLowReason;
import com.yapp.d14.interview.domain.TestType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "axis_evaluation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AxisEvaluationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    private TestType testType;

    @Column(name = "score")
    private Integer score;

    @Column(name = "applied_cap")
    private Integer appliedCap;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_level")
    private ResolutionLevel resolutionLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_low_reason")
    private ResolutionLowReason resolutionLowReason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "axis_evaluation_evidence_timestamp", joinColumns = @JoinColumn(name = "axis_evaluation_id"))
    private List<TimeRangeEmbeddable> evidenceTimestamps = new ArrayList<>();

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static AxisEvaluationJpaEntity from(AxisEvaluation axisEvaluation) {
        AxisEvaluationJpaEntity entity = new AxisEvaluationJpaEntity();
        entity.id = axisEvaluation.getId();
        entity.sessionId = axisEvaluation.getSessionId();
        entity.testType = axisEvaluation.getTestType();
        entity.score = axisEvaluation.getScore();
        entity.appliedCap = axisEvaluation.getAppliedCap();
        entity.resolutionLevel = axisEvaluation.getResolutionLevel();
        entity.resolutionLowReason = axisEvaluation.getResolutionLowReason();
        entity.evidenceTimestamps = axisEvaluation.getEvidenceTimestamps().stream()
                .map(TimeRangeEmbeddable::from)
                .toList();
        entity.rationale = axisEvaluation.getRationale();
        entity.createdAt = axisEvaluation.getCreatedAt();
        return entity;
    }

    public AxisEvaluation toDomain() {
        return AxisEvaluation.of(
                id,
                sessionId,
                testType,
                score,
                appliedCap,
                resolutionLevel,
                resolutionLowReason,
                evidenceTimestamps.stream().map(TimeRangeEmbeddable::toDomain).toList(),
                rationale,
                createdAt
        );
    }
}

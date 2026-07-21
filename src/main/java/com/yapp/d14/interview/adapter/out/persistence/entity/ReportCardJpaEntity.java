package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.TestType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "report_card")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportCardJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "depth_level", nullable = false)
    private int depthLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    private TestType testType;

    @Column(name = "question_intent_translation", columnDefinition = "TEXT")
    private String questionIntentTranslation;

    @OneToMany(mappedBy = "reportCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @BatchSize(size = 100)
    private List<ReportCardHighlightJpaEntity> highlightSpans = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static ReportCardJpaEntity from(ReportCard reportCard) {
        ReportCardJpaEntity entity = new ReportCardJpaEntity();
        entity.id = reportCard.getId();
        entity.sessionId = reportCard.getSessionId();
        entity.questionId = reportCard.getQuestionId();
        entity.depthLevel = reportCard.getDepthLevel();
        entity.testType = reportCard.getTestType();
        entity.questionIntentTranslation = reportCard.getQuestionIntentTranslation();
        entity.highlightSpans = reportCard.getHighlightSpans().stream()
                .map(span -> ReportCardHighlightJpaEntity.from(entity, span))
                .toList();
        entity.createdAt = reportCard.getCreatedAt();
        return entity;
    }

    public ReportCard toDomain() {
        return ReportCard.of(
                id,
                sessionId,
                questionId,
                depthLevel,
                testType,
                questionIntentTranslation,
                highlightSpans.stream().map(ReportCardHighlightJpaEntity::toDomain).toList(),
                createdAt
        );
    }
}

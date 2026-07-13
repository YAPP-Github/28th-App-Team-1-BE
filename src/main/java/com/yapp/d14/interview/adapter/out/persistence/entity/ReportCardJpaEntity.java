package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.ReportCard;
import com.yapp.d14.interview.domain.RewriteSuggestion;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    private TestType testType;

    @Column(name = "question_intent_translation", columnDefinition = "TEXT")
    private String questionIntentTranslation;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_card_highlight_span", joinColumns = @JoinColumn(name = "report_card_id"))
    @BatchSize(size = 100)
    private List<HighlightSpanEmbeddable> highlightSpans = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_card_action_keyword", joinColumns = @JoinColumn(name = "report_card_id"))
    @BatchSize(size = 100)
    private List<ActionKeywordEmbeddable> actionKeywords = new ArrayList<>();

    @Column(name = "rewrite_original_quote", columnDefinition = "TEXT")
    private String rewriteOriginalQuote;

    @Column(name = "rewrite_rewritten_text", columnDefinition = "TEXT")
    private String rewriteRewrittenText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static ReportCardJpaEntity from(ReportCard reportCard) {
        ReportCardJpaEntity entity = new ReportCardJpaEntity();
        entity.id = reportCard.getId();
        entity.sessionId = reportCard.getSessionId();
        entity.testType = reportCard.getTestType();
        entity.questionIntentTranslation = reportCard.getQuestionIntentTranslation();
        entity.highlightSpans = reportCard.getHighlightSpans().stream()
                .map(HighlightSpanEmbeddable::from)
                .toList();
        entity.actionKeywords = reportCard.getActionKeywords().stream()
                .map(ActionKeywordEmbeddable::from)
                .toList();
        RewriteSuggestion rewriteSuggestion = reportCard.getRewriteSuggestion();
        entity.rewriteOriginalQuote = rewriteSuggestion == null ? null : rewriteSuggestion.originalQuote();
        entity.rewriteRewrittenText = rewriteSuggestion == null ? null : rewriteSuggestion.rewrittenText();
        entity.createdAt = reportCard.getCreatedAt();
        return entity;
    }

    public ReportCard toDomain() {
        RewriteSuggestion rewriteSuggestion = rewriteOriginalQuote == null
                ? null
                : new RewriteSuggestion(rewriteOriginalQuote, rewriteRewrittenText);

        return ReportCard.of(
                id,
                sessionId,
                testType,
                questionIntentTranslation,
                highlightSpans.stream().map(HighlightSpanEmbeddable::toDomain).toList(),
                actionKeywords.stream().map(ActionKeywordEmbeddable::toDomain).toList(),
                rewriteSuggestion,
                createdAt
        );
    }
}

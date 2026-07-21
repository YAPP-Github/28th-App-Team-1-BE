package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.TextRange;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "report_card_highlight")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportCardHighlightJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_card_id", nullable = false)
    private ReportCardJpaEntity reportCard;

    @Column(name = "start_index", nullable = false)
    private int startIndex;

    @Column(name = "end_index", nullable = false)
    private int endIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone", nullable = false)
    private HighlightTone tone;

    @OneToMany(mappedBy = "highlight", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @BatchSize(size = 100)
    private List<ReportCardActionKeywordJpaEntity> actionKeywords = new ArrayList<>();

    public static ReportCardHighlightJpaEntity from(ReportCardJpaEntity reportCard, HighlightSpan highlightSpan) {
        ReportCardHighlightJpaEntity entity = new ReportCardHighlightJpaEntity();
        entity.reportCard = reportCard;
        entity.startIndex = highlightSpan.range().startIndex();
        entity.endIndex = highlightSpan.range().endIndex();
        entity.tone = highlightSpan.tone();
        entity.actionKeywords = highlightSpan.actionKeywords() == null
                ? new ArrayList<>()
                : highlightSpan.actionKeywords().stream()
                        .map(actionKeyword -> ReportCardActionKeywordJpaEntity.from(entity, actionKeyword))
                        .toList();
        return entity;
    }

    public HighlightSpan toDomain() {
        return new HighlightSpan(
                new TextRange(startIndex, endIndex),
                tone,
                actionKeywords.stream().map(ReportCardActionKeywordJpaEntity::toDomain).toList()
        );
    }
}

package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.TextRange;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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

    @Column(name = "analysis", columnDefinition = "TEXT")
    private String analysis;

    public static ReportCardHighlightJpaEntity from(ReportCardJpaEntity reportCard, HighlightSpan highlightSpan) {
        ReportCardHighlightJpaEntity entity = new ReportCardHighlightJpaEntity();
        entity.reportCard = reportCard;
        entity.startIndex = highlightSpan.range().startIndex();
        entity.endIndex = highlightSpan.range().endIndex();
        entity.tone = highlightSpan.tone();
        entity.analysis = highlightSpan.analysis();
        return entity;
    }

    public HighlightSpan toDomain() {
        return new HighlightSpan(new TextRange(startIndex, endIndex), tone, analysis);
    }
}

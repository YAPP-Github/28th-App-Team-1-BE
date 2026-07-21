package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import com.yapp.d14.interview.domain.TextRange;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
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

    @Column(name = "analysis", columnDefinition = "TEXT")
    private String analysis;

    // 이 하이라이트에 대해 면접관이 이어서 던질 법한 추가 질문(0~3개). 순서 보존.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_card_highlight_follow_up", joinColumns = @JoinColumn(name = "report_card_highlight_id"))
    @OrderColumn(name = "idx")
    @Column(name = "question", columnDefinition = "TEXT")
    @BatchSize(size = 100)
    private List<String> followUpQuestions = new ArrayList<>();

    public static ReportCardHighlightJpaEntity from(ReportCardJpaEntity reportCard, HighlightSpan highlightSpan) {
        ReportCardHighlightJpaEntity entity = new ReportCardHighlightJpaEntity();
        entity.reportCard = reportCard;
        entity.startIndex = highlightSpan.range().startIndex();
        entity.endIndex = highlightSpan.range().endIndex();
        entity.tone = highlightSpan.tone();
        entity.analysis = highlightSpan.analysis();
        entity.followUpQuestions = highlightSpan.followUpQuestions() == null
                ? new ArrayList<>()
                : new ArrayList<>(highlightSpan.followUpQuestions());
        return entity;
    }

    public HighlightSpan toDomain() {
        return new HighlightSpan(new TextRange(startIndex, endIndex), tone, analysis, List.copyOf(followUpQuestions));
    }
}

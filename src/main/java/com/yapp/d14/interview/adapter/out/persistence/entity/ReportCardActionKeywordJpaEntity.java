package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.ActionKeyword;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "report_card_action_keyword")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportCardActionKeywordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "highlight_id", nullable = false)
    private ReportCardHighlightJpaEntity highlight;

    @Column(name = "keyword")
    private String keyword;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "rewritten_text", columnDefinition = "TEXT")
    private String rewrittenText;

    public static ReportCardActionKeywordJpaEntity from(ReportCardHighlightJpaEntity highlight, ActionKeyword actionKeyword) {
        ReportCardActionKeywordJpaEntity entity = new ReportCardActionKeywordJpaEntity();
        entity.highlight = highlight;
        entity.keyword = actionKeyword.keyword();
        entity.suggestion = actionKeyword.suggestion();
        entity.rewrittenText = actionKeyword.rewrittenText();
        return entity;
    }

    public ActionKeyword toDomain() {
        return new ActionKeyword(keyword, suggestion, rewrittenText);
    }
}

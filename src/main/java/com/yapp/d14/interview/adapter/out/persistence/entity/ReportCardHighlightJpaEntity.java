package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.HighlightReason;
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

    // 개선유형(#78). 카드 마무리 안내(A/B/C)와 followUpQuestions 노출 여부를 결정한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "reason")
    private HighlightReason reason;

    // 하이라이트 한 줄 제목(명사구).
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "analysis", columnDefinition = "TEXT")
    private String analysis;

    // 딴 답(reason=OFF_INTENT)일 때 답변이 실제로 다룬 주제 명사구. 그 외 reason에서는 null.
    @Column(name = "answer_topic_title", columnDefinition = "TEXT")
    private String answerTopicTitle;

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
        entity.reason = highlightSpan.reason();
        entity.title = highlightSpan.title();
        entity.analysis = highlightSpan.analysis();
        entity.answerTopicTitle = highlightSpan.answerTopicTitle();
        entity.followUpQuestions = highlightSpan.followUpQuestions() == null
                ? new ArrayList<>()
                : new ArrayList<>(highlightSpan.followUpQuestions());
        return entity;
    }

    public HighlightSpan toDomain() {
        return new HighlightSpan(new TextRange(startIndex, endIndex), tone, reason, title, analysis,
                List.copyOf(followUpQuestions), answerTopicTitle);
    }
}

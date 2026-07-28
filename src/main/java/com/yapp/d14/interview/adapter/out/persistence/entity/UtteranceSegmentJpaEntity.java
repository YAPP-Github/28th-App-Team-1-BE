package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.ScriptRole;
import com.yapp.d14.interview.domain.UtteranceSegment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// 문장 단위 발화 시각 1건(#78). 질문/답변을 role로 구분해 한 테이블에 담고, 리포트 조회 때 sessionId로 읽어 questionId로 카드에 매핑한다.
@Entity
@Table(name = "utterance_segment", indexes = @Index(name = "idx_utterance_segment_session", columnList = "session_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UtteranceSegmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ScriptRole role;

    @Column(name = "start_index", nullable = false)
    private int startIndex;

    @Column(name = "end_index", nullable = false)
    private int endIndex;

    @Column(name = "start_sec", nullable = false)
    private float startSec;

    @Column(name = "end_sec", nullable = false)
    private float endSec;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    public static UtteranceSegmentJpaEntity from(Long sessionId, Long questionId, UtteranceSegment segment) {
        UtteranceSegmentJpaEntity entity = new UtteranceSegmentJpaEntity();
        entity.sessionId = sessionId;
        entity.questionId = questionId;
        entity.role = segment.role();
        entity.startIndex = segment.startIndex();
        entity.endIndex = segment.endIndex();
        entity.startSec = segment.startSec();
        entity.endSec = segment.endSec();
        entity.text = segment.text();
        return entity;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public UtteranceSegment toDomain() {
        return new UtteranceSegment(role, text, startIndex, endIndex, startSec, endSec);
    }
}

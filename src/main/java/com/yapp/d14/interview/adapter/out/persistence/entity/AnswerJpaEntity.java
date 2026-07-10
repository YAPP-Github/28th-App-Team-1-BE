package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.Answer;
import com.yapp.d14.interview.domain.TestType;
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
@Table(name = "answer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "stt_text", columnDefinition = "TEXT")
    private String sttText;

    @Column(name = "answer_start_sec")
    private Float answerStartSec;

    @Column(name = "answer_end_sec")
    private Float answerEndSec;

    @Column(name = "answer_duration")
    private Float answerDuration;

    @Column(name = "is_skipped")
    private Boolean isSkipped;

    @Column(name = "stt_failure_ratio")
    private Float sttFailureRatio;

    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "voice_s3_key", length = 100)
    private String voiceS3Key;

    @Column(name = "ceiling_reached")
    private Boolean ceilingReached;

    @Column(name = "red_flag_detected")
    private Boolean redFlagDetected;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type")
    private TestType testType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static AnswerJpaEntity from(Answer answer) {
        AnswerJpaEntity entity = new AnswerJpaEntity();
        entity.id = answer.getId();
        entity.sessionId = answer.getSessionId();
        entity.questionId = answer.getQuestionId();
        entity.sttText = answer.getSttText();
        entity.answerStartSec = answer.getAnswerStartSec();
        entity.answerEndSec = answer.getAnswerEndSec();
        entity.answerDuration = answer.getAnswerDuration();
        entity.isSkipped = answer.getIsSkipped();
        entity.sttFailureRatio = answer.getSttFailureRatio();
        entity.evidenceSummary = answer.getEvidenceSummary();
        entity.rationale = answer.getRationale();
        entity.voiceS3Key = answer.getVoiceS3Key();
        entity.ceilingReached = answer.getCeilingReached();
        entity.redFlagDetected = answer.getRedFlagDetected();
        entity.testType = answer.getTestType();
        entity.createdAt = answer.getCreatedAt();
        return entity;
    }

    public Answer toDomain() {
        return Answer.of(
                id,
                sessionId,
                questionId,
                sttText,
                answerStartSec,
                answerEndSec,
                answerDuration,
                isSkipped,
                sttFailureRatio,
                evidenceSummary,
                rationale,
                voiceS3Key,
                ceilingReached,
                redFlagDetected,
                testType,
                createdAt
        );
    }
}

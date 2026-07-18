package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.Question;
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
@Table(name = "question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(columnDefinition = "TEXT")
    private String content;

    // 세션 전체 기준 질문 순서
    @Column(name = "turn_level")
    private Integer turnLevel;

    // 같은 testType 내에서의 질문 순서(꼬리 질문 깊이)
    @Column(name = "depth_level")
    private Integer depthLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type")
    private TestType testType;

    @Column(name = "applied_principle", length = 10)
    private String appliedPrinciple;

    // 질문 음성이 영상에서 시작/종료된 시점(초)
    @Column(name = "question_start_sec")
    private Float questionStartSec;

    @Column(name = "question_end_sec")
    private Float questionEndSec;

    @Column(name = "ai_voice_s3_key")
    private String aiVoiceS3Key;

    @Column(name = "is_wrap_up")
    private Boolean isWrapUp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static QuestionJpaEntity from(Question question) {
        QuestionJpaEntity entity = new QuestionJpaEntity();
        entity.id = question.getId();
        entity.sessionId = question.getSessionId();
        entity.content = question.getContent();
        entity.turnLevel = question.getTurnLevel();
        entity.depthLevel = question.getDepthLevel();
        entity.testType = question.getTestType();
        entity.appliedPrinciple = question.getAppliedPrinciple();
        entity.questionStartSec = question.getQuestionStartSec();
        entity.questionEndSec = question.getQuestionEndSec();
        entity.aiVoiceS3Key = question.getAiVoiceS3Key();
        entity.isWrapUp = question.getIsWrapUp();
        entity.createdAt = question.getCreatedAt();
        return entity;
    }

    public Question toDomain() {
        return Question.of(
                id,
                sessionId,
                content,
                turnLevel,
                depthLevel,
                testType,
                appliedPrinciple,
                questionStartSec,
                questionEndSec,
                aiVoiceS3Key,
                isWrapUp,
                createdAt
        );
    }
}

package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Question {

    private final Long id;
    private final Long sessionId;
    private final String content;

    // 세션 전체 기준 질문 순서
    private final Integer turnLevel;

    // 같은 testType 내에서의 질문 순서(꼬리 질문 깊이)
    private final Integer depthLevel;

    private final TestType testType;
    private final String appliedPrinciple;

    // 질문 음성이 영상에서 시작/종료된 시점(초)
    private Float questionStartSec;
    private Float questionEndSec;
    private final String aiVoiceS3Key;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Question(
            Long id,
            Long sessionId,
            String content,
            Integer turnLevel,
            Integer depthLevel,
            TestType testType,
            String appliedPrinciple,
            Float questionStartSec,
            Float questionEndSec,
            String aiVoiceS3Key,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.content = content;
        this.turnLevel = turnLevel;
        this.depthLevel = depthLevel;
        this.testType = testType;
        this.appliedPrinciple = appliedPrinciple;
        this.questionStartSec = questionStartSec;
        this.questionEndSec = questionEndSec;
        this.aiVoiceS3Key = aiVoiceS3Key;
        this.createdAt = createdAt;
    }

    public static Question create(
            Long sessionId,
            String content,
            Integer turnLevel,
            Integer depthLevel,
            TestType testType,
            String appliedPrinciple,
            String aiVoiceS3Key
    ) {
        return Question.builder()
                .sessionId(sessionId)
                .content(content)
                .turnLevel(turnLevel)
                .depthLevel(depthLevel)
                .testType(testType)
                .appliedPrinciple(appliedPrinciple)
                .aiVoiceS3Key(aiVoiceS3Key)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markPlayed(Float questionStartSec, Float questionEndSec) {
        validatePlaybackRange(questionStartSec, questionEndSec);
        this.questionStartSec = questionStartSec;
        this.questionEndSec = questionEndSec;
    }

    private static void validatePlaybackRange(Float startSec, Float endSec) {
        requireNonNegativeFinite(startSec, "questionStartSec");
        requireNonNegativeFinite(endSec, "questionEndSec");
        if (startSec != null && endSec != null && startSec > endSec) {
            throw new IllegalArgumentException(
                    "재생 시작 시간은 종료 시간보다 클 수 없어요. start=%s, end=%s".formatted(startSec, endSec)
            );
        }
    }

    private static void requireNonNegativeFinite(Float value, String fieldName) {
        if (value == null) {
            return;
        }
        if (value.isNaN() || value.isInfinite()) {
            throw new IllegalArgumentException("%s는 유한한 값이어야 해요. value=%s".formatted(fieldName, value));
        }
        if (value < 0) {
            throw new IllegalArgumentException("%s는 음수일 수 없어요. value=%s".formatted(fieldName, value));
        }
    }

    public static Question of(
            Long id,
            Long sessionId,
            String content,
            Integer turnLevel,
            Integer depthLevel,
            TestType testType,
            String appliedPrinciple,
            Float questionStartSec,
            Float questionEndSec,
            String aiVoiceS3Key,
            LocalDateTime createdAt
    ) {
        return Question.builder()
                .id(id)
                .sessionId(sessionId)
                .content(content)
                .turnLevel(turnLevel)
                .depthLevel(depthLevel)
                .testType(testType)
                .appliedPrinciple(appliedPrinciple)
                .questionStartSec(questionStartSec)
                .questionEndSec(questionEndSec)
                .aiVoiceS3Key(aiVoiceS3Key)
                .createdAt(createdAt)
                .build();
    }
}

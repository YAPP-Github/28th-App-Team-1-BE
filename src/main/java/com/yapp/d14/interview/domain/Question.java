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
    private final Integer indexLevel;

    // 같은 testType 내에서의 질문 순서(꼬리 질문 깊이)
    private final Integer depthLevel;

    private final TestType testType;
    private final String appliedPrinciple;

    // 질문 음성이 영상에서 시작/종료된 시점(초)
    private final Float questionStartSec;
    private final Float questionEndSec;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Question(
            Long id,
            Long sessionId,
            String content,
            Integer indexLevel,
            Integer depthLevel,
            TestType testType,
            String appliedPrinciple,
            Float questionStartSec,
            Float questionEndSec,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.content = content;
        this.indexLevel = indexLevel;
        this.depthLevel = depthLevel;
        this.testType = testType;
        this.appliedPrinciple = appliedPrinciple;
        this.questionStartSec = questionStartSec;
        this.questionEndSec = questionEndSec;
        this.createdAt = createdAt;
    }

    public static Question create(
            Long sessionId,
            String content,
            Integer indexLevel,
            Integer depthLevel,
            TestType testType,
            String appliedPrinciple
    ) {
        return Question.builder()
                .sessionId(sessionId)
                .content(content)
                .indexLevel(indexLevel)
                .depthLevel(depthLevel)
                .testType(testType)
                .appliedPrinciple(appliedPrinciple)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Question of(
            Long id,
            Long sessionId,
            String content,
            Integer indexLevel,
            Integer depthLevel,
            TestType testType,
            String appliedPrinciple,
            Float questionStartSec,
            Float questionEndSec,
            LocalDateTime createdAt
    ) {
        return Question.builder()
                .id(id)
                .sessionId(sessionId)
                .content(content)
                .indexLevel(indexLevel)
                .depthLevel(depthLevel)
                .testType(testType)
                .appliedPrinciple(appliedPrinciple)
                .questionStartSec(questionStartSec)
                .questionEndSec(questionEndSec)
                .createdAt(createdAt)
                .build();
    }
}

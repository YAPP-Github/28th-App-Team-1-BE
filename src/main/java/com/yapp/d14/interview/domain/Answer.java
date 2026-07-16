package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Answer {

    private final Long id;
    private final Long sessionId;
    private final Long questionId;
    private final String sttText;
    private final Float answerStartSec;
    private final Float answerEndSec;
    private final Float answerDuration;
    private final Boolean isSkipped;
    private final Float sttFailureRatio;
    private final String evidenceSummary;
    private final String rationale;
    private final String voiceS3Key;
    private final Boolean ceilingReached;
    private final Boolean redFlagDetected;
    private final TestType testType;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Answer(
            Long id,
            Long sessionId,
            Long questionId,
            String sttText,
            Float answerStartSec,
            Float answerEndSec,
            Float answerDuration,
            Boolean isSkipped,
            Float sttFailureRatio,
            String evidenceSummary,
            String rationale,
            String voiceS3Key,
            Boolean ceilingReached,
            Boolean redFlagDetected,
            TestType testType,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.sttText = sttText;
        this.answerStartSec = answerStartSec;
        this.answerEndSec = answerEndSec;
        this.answerDuration = answerDuration;
        this.isSkipped = isSkipped;
        this.sttFailureRatio = sttFailureRatio;
        this.evidenceSummary = evidenceSummary;
        this.rationale = rationale;
        this.voiceS3Key = voiceS3Key;
        this.ceilingReached = ceilingReached;
        this.redFlagDetected = redFlagDetected;
        this.testType = testType;
        this.createdAt = createdAt;
    }

    public static Answer create(
            Long sessionId,
            Long questionId,
            String sttText,
            Float answerStartSec,
            Float answerEndSec,
            Float answerDuration,
            Boolean isSkipped,
            Float sttFailureRatio,
            String evidenceSummary,
            String rationale,
            String voiceS3Key,
            Boolean ceilingReached,
            Boolean redFlagDetected,
            TestType testType
    ) {
        validateAnswerRange(answerStartSec, answerEndSec);
        return Answer.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .sttText(sttText)
                .answerStartSec(answerStartSec)
                .answerEndSec(answerEndSec)
                .answerDuration(answerDuration)
                .isSkipped(isSkipped)
                .sttFailureRatio(sttFailureRatio)
                .evidenceSummary(evidenceSummary)
                .rationale(rationale)
                .voiceS3Key(voiceS3Key)
                .ceilingReached(ceilingReached)
                .redFlagDetected(redFlagDetected)
                .testType(testType)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Answer of(
            Long id,
            Long sessionId,
            Long questionId,
            String sttText,
            Float answerStartSec,
            Float answerEndSec,
            Float answerDuration,
            Boolean isSkipped,
            Float sttFailureRatio,
            String evidenceSummary,
            String rationale,
            String voiceS3Key,
            Boolean ceilingReached,
            Boolean redFlagDetected,
            TestType testType,
            LocalDateTime createdAt
    ) {
        return Answer.builder()
                .id(id)
                .sessionId(sessionId)
                .questionId(questionId)
                .sttText(sttText)
                .answerStartSec(answerStartSec)
                .answerEndSec(answerEndSec)
                .answerDuration(answerDuration)
                .isSkipped(isSkipped)
                .sttFailureRatio(sttFailureRatio)
                .evidenceSummary(evidenceSummary)
                .rationale(rationale)
                .voiceS3Key(voiceS3Key)
                .ceilingReached(ceilingReached)
                .redFlagDetected(redFlagDetected)
                .testType(testType)
                .createdAt(createdAt)
                .build();
    }

    private static void validateAnswerRange(Float startSec, Float endSec) {
        requireNonNegativeFinite(startSec, "answerStartSec");
        requireNonNegativeFinite(endSec, "answerEndSec");
        if (startSec != null && endSec != null && startSec > endSec) {
            throw new IllegalArgumentException(
                    "답변 시작 시간은 종료 시간보다 클 수 없어요. start=%s, end=%s".formatted(startSec, endSec)
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
}

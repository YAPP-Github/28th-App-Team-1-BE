package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class RedFlag {

    private final Long id;
    private final Long sessionId;
    private final RedFlagType type;
    private final TestType affectedTestType;
    private final Integer capValue;
    private final boolean knockout;
    private final List<TimeRange> evidenceTimestamps;
    // 이 레드플래그가 걸린 질문(카드) 식별자들. affectedTestType이 없는(예: CONTRADICTION)
    // 레드플래그를 리포트 카드에 이어붙이는 연결고리로 쓴다. 매핑할 턴이 없으면 빈 리스트.
    private final List<Long> relatedQuestionIds;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RedFlag(
            Long id,
            Long sessionId,
            RedFlagType type,
            TestType affectedTestType,
            Integer capValue,
            boolean knockout,
            List<TimeRange> evidenceTimestamps,
            List<Long> relatedQuestionIds,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.type = type;
        this.affectedTestType = affectedTestType;
        this.capValue = capValue;
        this.knockout = knockout;
        this.evidenceTimestamps = evidenceTimestamps;
        this.relatedQuestionIds = relatedQuestionIds == null ? List.of() : relatedQuestionIds;
        this.createdAt = createdAt;
    }

    public static RedFlag create(
            Long sessionId,
            RedFlagType type,
            TestType affectedTestType,
            Integer capValue,
            boolean knockout,
            List<TimeRange> evidenceTimestamps,
            List<Long> relatedQuestionIds
    ) {
        return RedFlag.builder()
                .sessionId(sessionId)
                .type(type)
                .affectedTestType(affectedTestType)
                .capValue(capValue)
                .knockout(knockout)
                .evidenceTimestamps(evidenceTimestamps)
                .relatedQuestionIds(relatedQuestionIds)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static RedFlag of(
            Long id,
            Long sessionId,
            RedFlagType type,
            TestType affectedTestType,
            Integer capValue,
            boolean knockout,
            List<TimeRange> evidenceTimestamps,
            List<Long> relatedQuestionIds,
            LocalDateTime createdAt
    ) {
        return RedFlag.builder()
                .id(id)
                .sessionId(sessionId)
                .type(type)
                .affectedTestType(affectedTestType)
                .capValue(capValue)
                .knockout(knockout)
                .evidenceTimestamps(evidenceTimestamps)
                .relatedQuestionIds(relatedQuestionIds)
                .createdAt(createdAt)
                .build();
    }
}

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
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.type = type;
        this.affectedTestType = affectedTestType;
        this.capValue = capValue;
        this.knockout = knockout;
        this.evidenceTimestamps = evidenceTimestamps;
        this.createdAt = createdAt;
    }

    public static RedFlag create(
            Long sessionId,
            RedFlagType type,
            TestType affectedTestType,
            Integer capValue,
            boolean knockout,
            List<TimeRange> evidenceTimestamps
    ) {
        return RedFlag.builder()
                .sessionId(sessionId)
                .type(type)
                .affectedTestType(affectedTestType)
                .capValue(capValue)
                .knockout(knockout)
                .evidenceTimestamps(evidenceTimestamps)
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
                .createdAt(createdAt)
                .build();
    }
}

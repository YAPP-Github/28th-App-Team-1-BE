package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class QuestionCandidate {

    private final Long id;
    private final Long sessionId;
    private final QuestionCandidateSource source;
    private final String sourceRef;
    private final TestType testType;
    private final TestType secondaryTestType;
    private final String probeText;
    private final String echoQuote;
    private final String jdMatch;
    private final QuestionCandidateStrength strength;
    private QuestionCandidateStatus status;
    private QuestionCandidateStaleReason staleReason;
    private Integer usedInTurn;
    private Integer contradictingTurnNumber;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private QuestionCandidate(
            Long id,
            Long sessionId,
            QuestionCandidateSource source,
            String sourceRef,
            TestType testType,
            TestType secondaryTestType,
            String probeText,
            String echoQuote,
            String jdMatch,
            QuestionCandidateStrength strength,
            QuestionCandidateStatus status,
            QuestionCandidateStaleReason staleReason,
            Integer usedInTurn,
            Integer contradictingTurnNumber,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.source = source;
        this.sourceRef = sourceRef;
        this.testType = testType;
        this.secondaryTestType = secondaryTestType;
        this.probeText = probeText;
        this.echoQuote = echoQuote;
        this.jdMatch = jdMatch;
        this.strength = strength;
        this.status = status;
        this.staleReason = staleReason;
        this.usedInTurn = usedInTurn;
        this.contradictingTurnNumber = contradictingTurnNumber;
        this.createdAt = createdAt;
    }

    public static QuestionCandidate create(
            Long sessionId,
            QuestionCandidateSource source,
            String sourceRef,
            TestType testType,
            TestType secondaryTestType,
            String probeText,
            String echoQuote,
            String jdMatch,
            QuestionCandidateStrength strength
    ) {
        return QuestionCandidate.builder()
                .sessionId(sessionId)
                .source(source)
                .sourceRef(sourceRef)
                .testType(testType)
                .secondaryTestType(secondaryTestType)
                .probeText(probeText)
                .echoQuote(echoQuote)
                .jdMatch(jdMatch)
                .strength(strength)
                .status(QuestionCandidateStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static QuestionCandidate of(
            Long id,
            Long sessionId,
            QuestionCandidateSource source,
            String sourceRef,
            TestType testType,
            TestType secondaryTestType,
            String probeText,
            String echoQuote,
            String jdMatch,
            QuestionCandidateStrength strength,
            QuestionCandidateStatus status,
            QuestionCandidateStaleReason staleReason,
            Integer usedInTurn,
            Integer contradictingTurnNumber,
            LocalDateTime createdAt
    ) {
        return QuestionCandidate.builder()
                .id(id)
                .sessionId(sessionId)
                .source(source)
                .sourceRef(sourceRef)
                .testType(testType)
                .secondaryTestType(secondaryTestType)
                .probeText(probeText)
                .echoQuote(echoQuote)
                .jdMatch(jdMatch)
                .strength(strength)
                .status(status)
                .staleReason(staleReason)
                .usedInTurn(usedInTurn)
                .contradictingTurnNumber(contradictingTurnNumber)
                .createdAt(createdAt)
                .build();
    }
}

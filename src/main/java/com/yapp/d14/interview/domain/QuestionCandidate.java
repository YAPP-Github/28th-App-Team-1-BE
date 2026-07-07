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
    private final TestType axis;
    private final TestType secondaryAxis;
    private final String probeText;
    private final String echoQuote;
    private final String jdMatch;
    private final QuestionCandidateStrength strength;
    private QuestionCandidateStatus status;
    private QuestionCandidateStaleReason staleReason;
    private Integer usedInTurn;
    private final LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private QuestionCandidate(
            Long id,
            Long sessionId,
            QuestionCandidateSource source,
            String sourceRef,
            TestType axis,
            TestType secondaryAxis,
            String probeText,
            String echoQuote,
            String jdMatch,
            QuestionCandidateStrength strength,
            QuestionCandidateStatus status,
            QuestionCandidateStaleReason staleReason,
            Integer usedInTurn,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.source = source;
        this.sourceRef = sourceRef;
        this.axis = axis;
        this.secondaryAxis = secondaryAxis;
        this.probeText = probeText;
        this.echoQuote = echoQuote;
        this.jdMatch = jdMatch;
        this.strength = strength;
        this.status = status;
        this.staleReason = staleReason;
        this.usedInTurn = usedInTurn;
        this.createdAt = createdAt;
    }

    public static QuestionCandidate create(
            Long sessionId,
            QuestionCandidateSource source,
            String sourceRef,
            TestType axis,
            TestType secondaryAxis,
            String probeText,
            String echoQuote,
            String jdMatch,
            QuestionCandidateStrength strength
    ) {
        return QuestionCandidate.builder()
                .sessionId(sessionId)
                .source(source)
                .sourceRef(sourceRef)
                .axis(axis)
                .secondaryAxis(secondaryAxis)
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
            TestType axis,
            TestType secondaryAxis,
            String probeText,
            String echoQuote,
            String jdMatch,
            QuestionCandidateStrength strength,
            QuestionCandidateStatus status,
            QuestionCandidateStaleReason staleReason,
            Integer usedInTurn,
            LocalDateTime createdAt
    ) {
        return QuestionCandidate.builder()
                .id(id)
                .sessionId(sessionId)
                .source(source)
                .sourceRef(sourceRef)
                .axis(axis)
                .secondaryAxis(secondaryAxis)
                .probeText(probeText)
                .echoQuote(echoQuote)
                .jdMatch(jdMatch)
                .strength(strength)
                .status(status)
                .staleReason(staleReason)
                .usedInTurn(usedInTurn)
                .createdAt(createdAt)
                .build();
    }
}

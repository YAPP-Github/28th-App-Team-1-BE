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

    // run_live_turn이 이 캐물지점을 만들 때 참고한 전술 id(P1~P24). 채점 미사용, 디버깅/검증용.
    private final String principleUsed;

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
            LocalDateTime createdAt,
            String principleUsed
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
        this.principleUsed = principleUsed;
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
            QuestionCandidateStrength strength,
            String principleUsed
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
                .principleUsed(principleUsed)
                .build();
    }

    // 이 캐물지점을 질문으로 사용 확정 (5-6장): status=USED 전환 + 사용된 turnLevel 기록
    public void markUsed(int usedInTurn) {
        this.status = QuestionCandidateStatus.USED;
        this.usedInTurn = usedInTurn;
    }

    // 5-3장: 모순/자진정정 감지 시 이 캐물지점을 stale 처리. contradicted면 이 캐물지점이 만들어졌던 시점과
    // 모순되는 답변이 나온 turnLevel을, corrected면 지원자가 스스로 정정한 turnLevel을 기록한다.
    public void markStale(QuestionCandidateStaleReason reason, Integer contradictingTurnNumber) {
        this.status = QuestionCandidateStatus.STALE;
        this.staleReason = reason;
        this.contradictingTurnNumber = contradictingTurnNumber;
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
            LocalDateTime createdAt,
            String principleUsed
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
                .principleUsed(principleUsed)
                .build();
    }
}

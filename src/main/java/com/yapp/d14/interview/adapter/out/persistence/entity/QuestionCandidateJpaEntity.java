package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStaleReason;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_candidates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionCandidateJpaEntity {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private QuestionCandidateSource source;

    @Column(name = "source_ref")
    private String sourceRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "axis")
    private TestType axis;

    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_axis")
    private TestType secondaryAxis;

    @Column(name = "probe_text", columnDefinition = "TEXT")
    private String probeText;

    @Column(name = "echo_quote", columnDefinition = "TEXT")
    private String echoQuote;

    @Column(name = "jd_match")
    private String jdMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "strength")
    private QuestionCandidateStrength strength;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private QuestionCandidateStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "stale_reason")
    private QuestionCandidateStaleReason staleReason;

    @Column(name = "used_in_turn")
    private Integer usedInTurn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static QuestionCandidateJpaEntity from(QuestionCandidate questionCandidate) {
        QuestionCandidateJpaEntity entity = new QuestionCandidateJpaEntity();
        entity.id = questionCandidate.getId();
        entity.sessionId = questionCandidate.getSessionId();
        entity.source = questionCandidate.getSource();
        entity.sourceRef = questionCandidate.getSourceRef();
        entity.axis = questionCandidate.getAxis();
        entity.secondaryAxis = questionCandidate.getSecondaryAxis();
        entity.probeText = questionCandidate.getProbeText();
        entity.echoQuote = questionCandidate.getEchoQuote();
        entity.jdMatch = questionCandidate.getJdMatch();
        entity.strength = questionCandidate.getStrength();
        entity.status = questionCandidate.getStatus();
        entity.staleReason = questionCandidate.getStaleReason();
        entity.usedInTurn = questionCandidate.getUsedInTurn();
        entity.createdAt = questionCandidate.getCreatedAt();
        return entity;
    }

    public QuestionCandidate toDomain() {
        return QuestionCandidate.of(
                id,
                sessionId,
                source,
                sourceRef,
                axis,
                secondaryAxis,
                probeText,
                echoQuote,
                jdMatch,
                strength,
                status,
                staleReason,
                usedInTurn,
                createdAt
        );
    }
}

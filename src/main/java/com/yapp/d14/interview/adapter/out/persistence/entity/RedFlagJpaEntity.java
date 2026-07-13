package com.yapp.d14.interview.adapter.out.persistence.entity;

import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.TestType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "red_flag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RedFlagJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RedFlagType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "affected_test_type")
    private TestType affectedTestType;

    @Column(name = "cap_value")
    private Integer capValue;

    @Column(name = "knockout", nullable = false)
    private boolean knockout;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "red_flag_evidence_timestamp", joinColumns = @JoinColumn(name = "red_flag_id"))
    @BatchSize(size = 100)
    private List<TimeRangeEmbeddable> evidenceTimestamps = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static RedFlagJpaEntity from(RedFlag redFlag) {
        RedFlagJpaEntity entity = new RedFlagJpaEntity();
        entity.id = redFlag.getId();
        entity.sessionId = redFlag.getSessionId();
        entity.type = redFlag.getType();
        entity.affectedTestType = redFlag.getAffectedTestType();
        entity.capValue = redFlag.getCapValue();
        entity.knockout = redFlag.isKnockout();
        entity.evidenceTimestamps = redFlag.getEvidenceTimestamps().stream()
                .map(TimeRangeEmbeddable::from)
                .toList();
        entity.createdAt = redFlag.getCreatedAt();
        return entity;
    }

    public RedFlag toDomain() {
        return RedFlag.of(
                id,
                sessionId,
                type,
                affectedTestType,
                capValue,
                knockout,
                evidenceTimestamps.stream().map(TimeRangeEmbeddable::toDomain).toList(),
                createdAt
        );
    }
}

package com.yapp.d14.consent.adapter.out.persistence.entity;

import com.yapp.d14.consent.domain.ConsentItem;
import com.yapp.d14.consent.domain.ConsentRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consent_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentRecordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentItem item;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private boolean agreed;

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    public static ConsentRecordJpaEntity from(ConsentRecord consentRecord) {
        ConsentRecordJpaEntity entity = new ConsentRecordJpaEntity();
        entity.userId = consentRecord.getUserId();
        entity.item = consentRecord.getItem();
        entity.version = consentRecord.getVersion();
        entity.agreed = consentRecord.isAgreed();
        entity.respondedAt = consentRecord.getRespondedAt();
        return entity;
    }

    public ConsentRecord toDomain() {
        return ConsentRecord.of(userId, item, version, agreed, respondedAt);
    }
}

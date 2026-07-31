package com.yapp.d14.consent.adapter.out.persistence;

import com.yapp.d14.consent.adapter.out.persistence.entity.ConsentRecordJpaEntity;
import com.yapp.d14.consent.application.port.out.ConsentRecordRepository;
import com.yapp.d14.consent.domain.ConsentRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class ConsentRecordPersistenceAdapter implements ConsentRecordRepository {

    private final ConsentRecordJpaRepository consentRecordJpaRepository;

    @Override
    public ConsentRecord save(ConsentRecord consentRecord) {
        return consentRecordJpaRepository.save(ConsentRecordJpaEntity.from(consentRecord)).toDomain();
    }

    @Override
    public List<ConsentRecord> findAllByUserId(UUID userId) {
        return consentRecordJpaRepository.findAllByUserId(userId).stream()
                .map(ConsentRecordJpaEntity::toDomain)
                .toList();
    }
}

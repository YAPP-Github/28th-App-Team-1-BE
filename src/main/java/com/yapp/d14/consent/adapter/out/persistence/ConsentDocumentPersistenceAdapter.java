package com.yapp.d14.consent.adapter.out.persistence;

import com.yapp.d14.consent.adapter.out.persistence.entity.ConsentDocumentJpaEntity;
import com.yapp.d14.consent.application.port.out.ConsentDocumentRepository;
import com.yapp.d14.consent.domain.ConsentDocument;
import com.yapp.d14.consent.domain.ConsentItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ConsentDocumentPersistenceAdapter implements ConsentDocumentRepository {

    private final ConsentDocumentJpaRepository consentDocumentJpaRepository;

    @Override
    public Optional<ConsentDocument> findByItemAndVersion(ConsentItem item, int version) {
        return consentDocumentJpaRepository.findByItemAndVersion(item, version).map(ConsentDocumentJpaEntity::toDomain);
    }

    @Override
    public boolean existsByItemAndVersion(ConsentItem item, int version) {
        return consentDocumentJpaRepository.existsByItemAndVersion(item, version);
    }
}

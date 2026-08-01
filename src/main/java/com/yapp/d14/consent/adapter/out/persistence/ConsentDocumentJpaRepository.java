package com.yapp.d14.consent.adapter.out.persistence;

import com.yapp.d14.consent.adapter.out.persistence.entity.ConsentDocumentJpaEntity;
import com.yapp.d14.consent.domain.ConsentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ConsentDocumentJpaRepository extends JpaRepository<ConsentDocumentJpaEntity, Long> {

    Optional<ConsentDocumentJpaEntity> findByItemAndVersion(ConsentItem item, int version);

    boolean existsByItemAndVersion(ConsentItem item, int version);
}

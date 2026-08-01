package com.yapp.d14.consent.adapter.out.persistence;

import com.yapp.d14.consent.adapter.out.persistence.entity.ConsentRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ConsentRecordJpaRepository extends JpaRepository<ConsentRecordJpaEntity, Long> {

    List<ConsentRecordJpaEntity> findAllByUserId(UUID userId);
}

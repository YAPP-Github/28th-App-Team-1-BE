package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.ReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ReportJpaRepository extends JpaRepository<ReportJpaEntity, Long> {

    Optional<ReportJpaEntity> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}

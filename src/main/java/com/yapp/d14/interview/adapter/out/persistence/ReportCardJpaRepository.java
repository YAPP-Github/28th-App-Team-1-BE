package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.ReportCardJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ReportCardJpaRepository extends JpaRepository<ReportCardJpaEntity, Long> {

    List<ReportCardJpaEntity> findAllBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}

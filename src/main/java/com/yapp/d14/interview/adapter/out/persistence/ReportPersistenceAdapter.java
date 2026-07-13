package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.ReportJpaEntity;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ReportPersistenceAdapter implements ReportRepository {

    private final ReportJpaRepository reportJpaRepository;

    @Override
    public Report save(Report report) {
        return reportJpaRepository.save(ReportJpaEntity.from(report)).toDomain();
    }

    @Override
    public Optional<Report> findBySessionId(Long sessionId) {
        return reportJpaRepository.findBySessionId(sessionId).map(ReportJpaEntity::toDomain);
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        reportJpaRepository.deleteBySessionId(sessionId);
    }
}

package com.yapp.d14.interview.adapter.out.persistence;

import com.yapp.d14.interview.adapter.out.persistence.entity.ReportCardJpaEntity;
import com.yapp.d14.interview.application.port.out.ReportCardRepository;
import com.yapp.d14.interview.domain.ReportCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class ReportCardPersistenceAdapter implements ReportCardRepository {

    private final ReportCardJpaRepository reportCardJpaRepository;

    @Override
    public ReportCard save(ReportCard reportCard) {
        return reportCardJpaRepository.save(ReportCardJpaEntity.from(reportCard)).toDomain();
    }

    @Override
    public List<ReportCard> findAllBySessionId(Long sessionId) {
        return reportCardJpaRepository.findAllBySessionId(sessionId).stream()
                .map(ReportCardJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        reportCardJpaRepository.deleteBySessionId(sessionId);
    }
}

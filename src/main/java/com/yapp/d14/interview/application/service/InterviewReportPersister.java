package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.AxisEvaluationRepository;
import com.yapp.d14.interview.application.port.out.RedFlagRepository;
import com.yapp.d14.interview.application.port.out.ReportCardRepository;
import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.AxisEvaluation;
import com.yapp.d14.interview.domain.RedFlag;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class InterviewReportPersister {

    private final ReportRepository reportRepository;
    private final AxisEvaluationRepository axisEvaluationRepository;
    private final RedFlagRepository redFlagRepository;
    private final ReportCardRepository reportCardRepository;

    @Transactional
    void persist(
            Long sessionId,
            Report report,
            List<AxisEvaluation> axisEvaluations,
            List<RedFlag> redFlags,
            List<ReportCard> reportCards
    ) {
        reportRepository.deleteBySessionId(sessionId);
        axisEvaluationRepository.deleteBySessionId(sessionId);
        redFlagRepository.deleteBySessionId(sessionId);
        reportCardRepository.deleteBySessionId(sessionId);

        reportRepository.save(report);
        axisEvaluationRepository.saveAll(axisEvaluations);
        redFlagRepository.saveAll(redFlags);
        reportCardRepository.saveAll(reportCards);
    }
}

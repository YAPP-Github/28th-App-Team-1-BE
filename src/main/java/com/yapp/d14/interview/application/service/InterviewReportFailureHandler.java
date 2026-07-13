package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.out.ReportRepository;
import com.yapp.d14.interview.domain.Report;
import com.yapp.d14.interview.domain.ReportStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
class InterviewReportFailureHandler {

    private final ReportRepository reportRepository;

    @Transactional
    void markFailed(Long sessionId) {
        reportRepository.deleteBySessionId(sessionId);
        Report report = Report.create(sessionId, null, null, null, null, ReportStatus.FAILED);
        reportRepository.save(report);
    }
}

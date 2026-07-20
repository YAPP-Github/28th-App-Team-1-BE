package com.yapp.d14.interview.application.service;

import com.yapp.d14.interview.application.port.in.InterviewReportQueryUseCase;
import com.yapp.d14.interview.application.port.in.result.InterviewReportQueryResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class InterviewReportQueryService implements InterviewReportQueryUseCase {

    @Override
    public InterviewReportQueryResult getReport(UUID userId, Long sessionId) {
        throw new UnsupportedOperationException("구현 예정 - #32 2단계");
    }
}

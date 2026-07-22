package com.yapp.d14.interview.application.port.in;

import com.yapp.d14.interview.application.port.in.result.InterviewReportQueryResult;

import java.util.UUID;

public interface InterviewReportQueryUseCase {

    InterviewReportQueryResult getReport(UUID userId, Long sessionId);
}

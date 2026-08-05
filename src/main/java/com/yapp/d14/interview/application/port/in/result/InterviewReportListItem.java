package com.yapp.d14.interview.application.port.in.result;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.domain.ReportStatus;

import java.time.LocalDateTime;

public record InterviewReportListItem(
        Long sessionId,
        JobType jobType,
        Integer careerYears,
        LocalDateTime interviewedAt,
        String portfolioFileName,
        boolean portfolioDeleted,
        String jdUrl,
        ReportStatus reportStatus,
        boolean feedbackAvailable,
        String title
) {
}

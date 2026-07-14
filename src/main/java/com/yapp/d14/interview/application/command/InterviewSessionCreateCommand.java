package com.yapp.d14.interview.application.command;

import com.yapp.d14.interview.domain.JobType;
import com.yapp.d14.interview.exception.InterviewErrorCode;
import com.yapp.d14.interview.exception.InterviewException;

import java.util.UUID;

public record InterviewSessionCreateCommand(
        UUID userId,
        UUID portfolioId,
        JobType jobRole,
        int careerYears,
        String jdUrl,
        String jdText,
        String freeText
) {

    public static InterviewSessionCreateCommand of(
            UUID userId,
            UUID portfolioId,
            String rawJobRole,
            Integer careerYears,
            String jdUrl,
            String jdText,
            String freeText
    ) {
        JobType jobRole = parseJobRole(rawJobRole);

        if (careerYears == null || careerYears < 0) {
            throw new InterviewException(InterviewErrorCode.INVALID_CAREER_YEARS);
        }

        return new InterviewSessionCreateCommand(userId, portfolioId, jobRole, careerYears, jdUrl, jdText, freeText);
    }

    private static JobType parseJobRole(String rawJobRole) {
        try {
            return JobType.valueOf(rawJobRole);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InterviewException(InterviewErrorCode.INVALID_JOB_ROLE);
        }
    }
}

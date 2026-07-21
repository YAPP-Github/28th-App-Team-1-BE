package com.yapp.d14.user.application.command;

import com.yapp.d14.job.domain.Job;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;

import java.util.UUID;

public record UserProfileUpdateCommand(UUID userId, String name, Job jobRole, Integer careerYears) {

    public static UserProfileUpdateCommand of(UUID userId, String name, String rawJobRole, Integer careerYears) {
        return new UserProfileUpdateCommand(userId, name, parseJobRole(rawJobRole), careerYears);
    }

    private static Job parseJobRole(String rawJobRole) {
        try {
            return Job.valueOf(rawJobRole);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new UserException(UserErrorCode.INVALID_JOB_ROLE);
        }
    }
}

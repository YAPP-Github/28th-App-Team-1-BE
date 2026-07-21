package com.yapp.d14.user.application.command;

import com.yapp.d14.user.domain.JobRole;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;

import java.util.UUID;

public record UserProfileUpdateCommand(UUID userId, String name, JobRole jobRole, Integer careerYears) {

    public static UserProfileUpdateCommand of(UUID userId, String name, String rawJobRole, Integer careerYears) {
        return new UserProfileUpdateCommand(userId, name, parseJobRole(rawJobRole), careerYears);
    }

    private static JobRole parseJobRole(String rawJobRole) {
        try {
            return JobRole.valueOf(rawJobRole);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new UserException(UserErrorCode.INVALID_JOB_ROLE);
        }
    }
}

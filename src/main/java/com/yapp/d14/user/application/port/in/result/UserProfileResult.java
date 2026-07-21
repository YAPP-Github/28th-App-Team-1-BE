package com.yapp.d14.user.application.port.in.result;

import com.yapp.d14.job.domain.Job;

import java.util.UUID;

public record UserProfileResult(
        UUID userId,
        String email,
        String name,
        boolean nameRegistered,
        Job jobRole,
        Integer careerYears,
        int remainingTicketCount
) {
}

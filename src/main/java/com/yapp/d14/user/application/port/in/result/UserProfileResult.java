package com.yapp.d14.user.application.port.in.result;

import com.yapp.d14.user.domain.JobRole;
import com.yapp.d14.user.domain.Provider;

import java.util.UUID;

public record UserProfileResult(
        UUID userId,
        String email,
        Provider provider,
        String name,
        boolean profileRegistered,
        JobRole jobRole,
        Integer careerYears,
        int remainingTicketCount
) {
}

package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.InterviewSessionStatus;

import java.util.UUID;

public record FileCleanupTarget(Long sessionId, UUID userId, InterviewSessionStatus status) {
}

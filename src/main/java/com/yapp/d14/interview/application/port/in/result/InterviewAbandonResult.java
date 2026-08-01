package com.yapp.d14.interview.application.port.in.result;

import com.yapp.d14.interview.domain.AbandonCause;

import java.time.LocalDateTime;

public record InterviewAbandonResult(
        Long sessionId,
        String status,
        AbandonCause abandonCause,
        LocalDateTime endedAt,
        String ticketOutcome,
        boolean reportGenerating
) {
}

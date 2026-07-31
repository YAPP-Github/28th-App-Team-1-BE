package com.yapp.d14.interview.application.command;

import java.util.UUID;

public record InterviewSessionCreateCommand(
        UUID userId,
        UUID portfolioId,
        String jdUrl,
        String jdText,
        String freeText
) {
}

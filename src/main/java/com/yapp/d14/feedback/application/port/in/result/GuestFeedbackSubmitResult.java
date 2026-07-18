package com.yapp.d14.feedback.application.port.in.result;

import java.time.LocalDateTime;

public record GuestFeedbackSubmitResult(
        Long submissionId,
        LocalDateTime submittedAt
) {
}

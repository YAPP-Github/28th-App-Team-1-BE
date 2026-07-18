package com.yapp.d14.interview.application.port.in.result;

import java.time.LocalDateTime;

public record InterviewVideoStatusResult(
        LocalDateTime expiresAt,
        boolean deleted
) {
}

package com.yapp.d14.interview.application.port.in.result;

import com.yapp.d14.interview.domain.InterviewSessionStatus;

public record InterviewSessionCreateResult(
        Long sessionId,
        InterviewSessionStatus status
) {
}

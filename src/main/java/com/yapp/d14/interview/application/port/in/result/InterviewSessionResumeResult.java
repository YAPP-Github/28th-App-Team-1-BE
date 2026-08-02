package com.yapp.d14.interview.application.port.in.result;

import java.time.LocalDateTime;

public record InterviewSessionResumeResult(
        String resumeState,
        LocalDateTime startedAt,
        Long elapsedSeconds,
        String status
) {

    public static InterviewSessionResumeResult resumable(LocalDateTime startedAt, long elapsedSeconds) {
        return new InterviewSessionResumeResult("RESUMABLE", startedAt, elapsedSeconds, null);
    }

    public static InterviewSessionResumeResult ended(String status) {
        return new InterviewSessionResumeResult("ENDED", null, null, status);
    }
}

package com.yapp.d14.interview.application.port.in.result;

import java.time.LocalDateTime;

public record InterviewSessionStatusResult(
        InterviewSessionPollStatus status,
        LocalDateTime startedAt,
        SummaryQuestion summaryQuestion
) {

    public record SummaryQuestion(Long questionId, String ttsAudio, int turnLevel, int depthLevel) {
    }
}

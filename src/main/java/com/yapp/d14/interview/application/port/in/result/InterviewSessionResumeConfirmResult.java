package com.yapp.d14.interview.application.port.in.result;

import com.yapp.d14.interview.domain.AbandonCause;
import com.yapp.d14.interview.domain.InterviewEndType;

import java.time.LocalDateTime;

public record InterviewSessionResumeConfirmResult(
        InterviewAnswerSubmitResult.NextQuestion nextQuestion,
        boolean sessionEnded,
        InterviewAnswerSubmitResult.WrapUpMessage wrapUpMessage,
        InterviewEndType endType,
        String status,
        AbandonCause abandonCause,
        LocalDateTime endedAt
) {

    public static InterviewSessionResumeConfirmResult nextQuestion(InterviewAnswerSubmitResult.NextQuestion nextQuestion) {
        return new InterviewSessionResumeConfirmResult(nextQuestion, false, null, null, null, null, null);
    }

    public static InterviewSessionResumeConfirmResult holdExpired(String status, AbandonCause abandonCause, LocalDateTime endedAt) {
        return new InterviewSessionResumeConfirmResult(null, true, null, null, status, abandonCause, endedAt);
    }
}

package com.yapp.d14.interview.application.port.in.result;

import com.yapp.d14.interview.domain.InterviewEndType;

public record InterviewAnswerSubmitResult(
        Long answerId,
        NextQuestion nextQuestion,
        boolean sessionEnded,
        WrapUpMessage wrapUpMessage,
        InterviewEndType endType
) {

    public record NextQuestion(Long questionId, boolean isLast, int turnLevel, int depthLevel) {
    }

    public record WrapUpMessage(String ttsAudio) {
    }
}

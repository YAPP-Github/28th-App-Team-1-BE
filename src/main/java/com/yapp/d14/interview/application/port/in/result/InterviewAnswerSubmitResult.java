package com.yapp.d14.interview.application.port.in.result;

public record InterviewAnswerSubmitResult(
        Long answerId,
        NextQuestion nextQuestion,
        boolean sessionEnded,
        WrapUpMessage wrapUpMessage,
        Long reportId
) {

    public record NextQuestion(Long questionId, boolean isLast, int turnLevel, int depthLevel) {
    }

    public record WrapUpMessage(String ttsAudio) {
    }
}

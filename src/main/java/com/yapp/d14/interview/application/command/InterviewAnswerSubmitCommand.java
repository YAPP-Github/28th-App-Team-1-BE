package com.yapp.d14.interview.application.command;

// 답변 제출(POST /answers) 유스케이스 입력. clientRequestId(idempotency)는 이번 구현 범위에서 제외.
// endType·isWrapUp 등 종료 분기 관련 필드는 이슈2(진입 처리)에서 추가된다.
public record InterviewAnswerSubmitCommand(
        Long sessionId,
        Long questionId,
        int turnLevel,
        byte[] audioContent,
        Float questionAudioStartSec,
        Float questionAudioEndSec,
        Float answerStartSec,
        Float answerEndSec,
        Float answerDuration
) {
}

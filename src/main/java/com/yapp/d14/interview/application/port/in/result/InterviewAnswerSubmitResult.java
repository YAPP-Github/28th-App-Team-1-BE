package com.yapp.d14.interview.application.port.in.result;

// POST /answers 응답 (설계 문서 7-2장). wrapUpMessage/reportId는 항상 null — 종료 처리는 이슈2 이후에서 채운다.
// wrapUpMessage의 실제 타입은 미정(설계문서 7-2장 "미정" 참고)이라 우선 Object로 둔다.
public record InterviewAnswerSubmitResult(
        Long answerId,
        NextQuestion nextQuestion,
        Object wrapUpMessage,
        Long reportId
) {

    public record NextQuestion(Long questionId, boolean isLast, int turnLevel, int depthLevel) {
    }
}

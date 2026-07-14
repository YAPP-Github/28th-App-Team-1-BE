package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import io.swagger.v3.oas.annotations.media.Schema;

// POST /answers 응답(설계 문서 7-2장). 방법 2-1 채택으로 ttsAudio는 없고, 오디오는 GET .../audio/stream으로 별도 조회한다.
public record InterviewAnswerSubmitHttpResponse(
        @Schema(description = "방금 처리된 답변의 식별자")
        Long answerId,

        @Schema(description = "다음 질문. null이면 세션 종료")
        NextQuestionHttpResponse nextQuestion,

        @Schema(description = "마무리 음성. 세션 종료 응답에서만 값 존재 (반환 형식 미정)")
        Object wrapUpMessage,

        @Schema(description = "세션 종료 시 발급되는 리포트 ID")
        Long reportId
) {

    public record NextQuestionHttpResponse(Long questionId, boolean isLast, Turn turn) {
    }

    public record Turn(int turnLevel, int depthLevel) {
    }

    public static InterviewAnswerSubmitHttpResponse from(InterviewAnswerSubmitResult result) {
        InterviewAnswerSubmitResult.NextQuestion nq = result.nextQuestion();
        NextQuestionHttpResponse nextQuestion = nq == null ? null : new NextQuestionHttpResponse(
                nq.questionId(), nq.isLast(), new Turn(nq.turnLevel(), nq.depthLevel())
        );
        return new InterviewAnswerSubmitHttpResponse(
                result.answerId(), nextQuestion, result.wrapUpMessage(), result.reportId()
        );
    }
}

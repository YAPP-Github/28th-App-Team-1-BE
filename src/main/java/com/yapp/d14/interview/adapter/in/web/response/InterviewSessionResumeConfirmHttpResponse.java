package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeConfirmResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record InterviewSessionResumeConfirmHttpResponse(
        @Schema(description = "이어서 답할 질문. 세션이 종료됐으면 null")
        NextQuestionHttpResponse nextQuestion,

        @Schema(description = "면접 세션이 완전히 종료됐는지 여부(hold 만료 레이스 발생 시 true)")
        boolean sessionEnded,

        @Schema(description = "마무리 멘트 음성. 이 흐름에서는 항상 null")
        WrapUpMessageHttpResponse wrapUpMessage,

        @Schema(description = "세션 종료 사유. 이 흐름에서는 항상 null")
        String endType,

        @Schema(description = "세션 종료 상태 — ABANDONED. sessionEnded=true일 때만 값 존재")
        String status,

        @Schema(description = "중단 사유 — HOLD_EXPIRED. sessionEnded=true일 때만 값 존재")
        String abandonCause,

        @Schema(description = "세션 종료 시각. sessionEnded=true일 때만 값 존재")
        LocalDateTime endedAt
) {

    public record NextQuestionHttpResponse(Long questionId, boolean isLast, Turn turn) {
    }

    public record Turn(int turnLevel, int depthLevel) {
    }

    public record WrapUpMessageHttpResponse(String ttsAudio) {
    }

    public static InterviewSessionResumeConfirmHttpResponse from(InterviewSessionResumeConfirmResult result) {
        InterviewAnswerSubmitResult.NextQuestion nq = result.nextQuestion();
        NextQuestionHttpResponse nextQuestion = nq == null ? null : new NextQuestionHttpResponse(
                nq.questionId(), nq.isLast(), new Turn(nq.turnLevel(), nq.depthLevel())
        );
        InterviewAnswerSubmitResult.WrapUpMessage wm = result.wrapUpMessage();
        WrapUpMessageHttpResponse wrapUpMessage = wm == null ? null : new WrapUpMessageHttpResponse(wm.ttsAudio());
        return new InterviewSessionResumeConfirmHttpResponse(
                nextQuestion,
                result.sessionEnded(),
                wrapUpMessage,
                result.endType() == null ? null : result.endType().name(),
                result.status(),
                result.abandonCause() == null ? null : result.abandonCause().name(),
                result.endedAt()
        );
    }
}

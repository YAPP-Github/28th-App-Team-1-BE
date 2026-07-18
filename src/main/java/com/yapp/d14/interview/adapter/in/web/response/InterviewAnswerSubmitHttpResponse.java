package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewAnswerSubmitResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record InterviewAnswerSubmitHttpResponse(
        @Schema(description = "방금 처리된 답변의 식별자")
        Long answerId,

        @Schema(description = "다음 질문. 세션이 종료됐으면 null")
        NextQuestionHttpResponse nextQuestion,

        @Schema(description = "면접 세션이 완전히 종료됐는지 여부")
        boolean sessionEnded,

        @Schema(description = "마무리 멘트 음성. 세션 종료 응답에서만 값 존재(EARLY_EXIT은 null)")
        WrapUpMessageHttpResponse wrapUpMessage,

        @Schema(description = "세션 종료 시 발급되는 리포트 ID")
        Long reportId
) {

    public record NextQuestionHttpResponse(Long questionId, boolean isLast, Turn turn) {
    }

    public record Turn(int turnLevel, int depthLevel) {
    }

    public record WrapUpMessageHttpResponse(
            @Schema(description = "마무리 멘트 음성(base64 인코딩된 mp3)")
            String ttsAudio
    ) {
    }

    public static InterviewAnswerSubmitHttpResponse from(InterviewAnswerSubmitResult result) {
        InterviewAnswerSubmitResult.NextQuestion nq = result.nextQuestion();
        NextQuestionHttpResponse nextQuestion = nq == null ? null : new NextQuestionHttpResponse(
                nq.questionId(), nq.isLast(), new Turn(nq.turnLevel(), nq.depthLevel())
        );
        InterviewAnswerSubmitResult.WrapUpMessage wm = result.wrapUpMessage();
        WrapUpMessageHttpResponse wrapUpMessage = wm == null ? null : new WrapUpMessageHttpResponse(wm.ttsAudio());
        return new InterviewAnswerSubmitHttpResponse(
                result.answerId(), nextQuestion, result.sessionEnded(), wrapUpMessage, result.reportId()
        );
    }
}

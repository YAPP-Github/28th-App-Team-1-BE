package com.yapp.d14.feedback.adapter.in.web.response;

import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackSubmitResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GuestFeedbackSubmitHttpResponse(
        @Schema(description = "제출 ID")
        Long submissionId,

        @Schema(description = "제출 시각")
        LocalDateTime submittedAt
) {

    public static GuestFeedbackSubmitHttpResponse from(GuestFeedbackSubmitResult result) {
        return new GuestFeedbackSubmitHttpResponse(
                result.submissionId(),
                result.submittedAt()
        );
    }
}

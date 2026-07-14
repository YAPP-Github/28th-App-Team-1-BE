package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionCreateResult;
import com.yapp.d14.interview.application.port.in.result.InterviewSessionPollStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record InterviewSessionCreateHttpResponse(
        @Schema(description = "생성된 면접 세션 ID")
        Long sessionId,

        @Schema(description = "세션 준비 상태, 생성 직후 항상 PROCESSING")
        String status,

        @Schema(description = "준비 상태 폴링 URL")
        String statusUrl
) {

    public static InterviewSessionCreateHttpResponse from(InterviewSessionCreateResult result) {
        return new InterviewSessionCreateHttpResponse(
                result.sessionId(),
                InterviewSessionPollStatus.from(result.status()).name(),
                "/api/v1/interview/sessions/%d/status".formatted(result.sessionId())
        );
    }
}

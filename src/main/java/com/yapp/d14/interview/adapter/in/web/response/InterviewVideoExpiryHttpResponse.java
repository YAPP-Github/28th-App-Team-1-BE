package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewVideoExpiryResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record InterviewVideoExpiryHttpResponse(
        @Schema(description = "영상 만료까지 남은 시간(초). 이미 만료됐으면 0", example = "2591480")
        long expiresInSeconds,

        @Schema(description = "만료 여부")
        boolean expired
) {

    public static InterviewVideoExpiryHttpResponse from(InterviewVideoExpiryResult result) {
        return new InterviewVideoExpiryHttpResponse(result.expiresInSeconds(), result.expired());
    }
}

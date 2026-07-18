package com.yapp.d14.feedback.adapter.in.web.response;

import com.yapp.d14.feedback.application.port.in.result.FeedbackShareCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record FeedbackShareCreateHttpResponse(
        @Schema(description = "공유 토큰. 클라이언트가 이 토큰으로 공유 딥링크를 조립한다.")
        String token
) {

    public static FeedbackShareCreateHttpResponse from(FeedbackShareCreateResult result) {
        return new FeedbackShareCreateHttpResponse(result.token());
    }
}

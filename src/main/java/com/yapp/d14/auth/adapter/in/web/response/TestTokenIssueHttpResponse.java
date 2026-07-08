package com.yapp.d14.auth.adapter.in.web.response;

import com.yapp.d14.auth.application.port.in.result.TestTokenIssueResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record TestTokenIssueHttpResponse(
        @Schema(description = "테스트용 JWT 액세스 토큰")
        String accessToken,

        @Schema(description = "토큰에 담긴 고정 테스트 사용자 ID (실제 user 테이블에 존재하지 않을 수 있음)")
        UUID userId,

        @Schema(description = "토큰에 담긴 임의의 provider")
        String provider
) {

    public static TestTokenIssueHttpResponse from(TestTokenIssueResult result) {
        return new TestTokenIssueHttpResponse(result.accessToken(), result.userId(), result.provider().name());
    }
}

package com.yapp.d14.auth.adapter.in.web.response;

import com.yapp.d14.auth.application.port.in.result.AuthToken;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthTokenHttpResponse(
        @Schema(description = "JWT 액세스 토큰 (유효 시간: 3시간)")
        String accessToken,

        @Schema(description = "리프레시 토큰 (유효 시간: 7일)")
        String refreshToken
) {

    public static AuthTokenHttpResponse from(AuthToken authToken) {
        return new AuthTokenHttpResponse(authToken.accessToken(), authToken.refreshToken());
    }
}

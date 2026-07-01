package com.yapp.d14.user.adapter.in.web.response;

import com.yapp.d14.user.application.port.in.AuthToken;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserSocialLoginHttpResponse(
        @Schema(description = "JWT 액세스 토큰 (유효 시간: 3시간)")
        String accessToken,

        @Schema(description = "리프레시 토큰 (유효 시간: 7일)")
        String refreshToken
) {

    public static UserSocialLoginHttpResponse from(AuthToken authToken) {
        return new UserSocialLoginHttpResponse(authToken.accessToken(), authToken.refreshToken());
    }
}

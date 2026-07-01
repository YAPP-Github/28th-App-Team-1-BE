package com.yapp.d14.user.adapter.in.web.response;

import com.yapp.d14.user.application.port.in.AuthToken;

public record UserSocialLoginHttpResponse(String accessToken, String refreshToken) {

    public static UserSocialLoginHttpResponse from(AuthToken authToken) {
        return new UserSocialLoginHttpResponse(authToken.accessToken(), authToken.refreshToken());
    }
}

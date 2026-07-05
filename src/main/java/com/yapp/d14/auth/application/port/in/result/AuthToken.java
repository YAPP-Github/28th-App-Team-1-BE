package com.yapp.d14.auth.application.port.in.result;

public record AuthToken(String accessToken, String refreshToken) {
}

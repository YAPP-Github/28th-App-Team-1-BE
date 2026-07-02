package com.yapp.d14.auth.application.port.in;

public record AuthToken(String accessToken, String refreshToken) {
}

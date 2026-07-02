package com.yapp.d14.auth.application.port.out;

import com.yapp.d14.user.domain.Provider;

import java.util.UUID;

public interface JwtProvider {

    String issueAccessToken(UUID userId, Provider provider);

    String issueRefreshToken(UUID userId, Provider provider);

    JwtClaims parseAccessToken(String token);

    JwtClaims parseRefreshToken(String token);
}

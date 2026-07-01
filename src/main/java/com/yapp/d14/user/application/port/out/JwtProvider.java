package com.yapp.d14.user.application.port.out;

import com.yapp.d14.user.domain.Provider;

import java.util.UUID;

public interface JwtProvider {

    String issueAccessToken(UUID userId, Provider provider);
}

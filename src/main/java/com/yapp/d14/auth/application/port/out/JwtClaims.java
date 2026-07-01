package com.yapp.d14.auth.application.port.out;

import com.yapp.d14.user.domain.Provider;

import java.util.UUID;

public record JwtClaims(UUID userId, Provider provider) {
}

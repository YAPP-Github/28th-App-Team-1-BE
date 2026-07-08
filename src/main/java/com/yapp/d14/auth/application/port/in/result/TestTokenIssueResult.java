package com.yapp.d14.auth.application.port.in.result;

import com.yapp.d14.user.domain.Provider;

import java.util.UUID;

public record TestTokenIssueResult(String accessToken, UUID userId, Provider provider) {
}

package com.yapp.d14.auth.application.command;

import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;

public record TokenReissueCommand(String refreshToken) {

    public TokenReissueCommand {
        if (refreshToken == null || refreshToken.isBlank()) throw new AuthException(AuthErrorCode.INVALID_TOKEN);
    }
}

package com.yapp.d14.auth.application.command;

import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.user.domain.Provider;

public record SocialLoginCommand(Provider provider, String credential) {

    public SocialLoginCommand {
        if (provider == null) throw new AuthException(AuthErrorCode.INVALID_CREDENTIAL);
        if (credential == null || credential.isBlank()) throw new AuthException(AuthErrorCode.INVALID_CREDENTIAL);
    }
}

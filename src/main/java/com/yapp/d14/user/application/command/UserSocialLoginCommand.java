package com.yapp.d14.user.application.command;

import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;

public record UserSocialLoginCommand(Provider provider, String credential) {

    public UserSocialLoginCommand {
        if (provider == null) throw new UserException(UserErrorCode.INVALID_CREDENTIAL);
        if (credential == null || credential.isBlank()) throw new UserException(UserErrorCode.INVALID_CREDENTIAL);
    }
}

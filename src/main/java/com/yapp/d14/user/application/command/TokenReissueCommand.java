package com.yapp.d14.user.application.command;

import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;

public record TokenReissueCommand(String refreshToken) {

    public TokenReissueCommand {
        if (refreshToken == null || refreshToken.isBlank()) throw new UserException(UserErrorCode.INVALID_TOKEN);
    }
}

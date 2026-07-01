package com.yapp.d14.auth.exception;

import com.yapp.d14.common.exception.BusinessException;

public class AuthException extends BusinessException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}

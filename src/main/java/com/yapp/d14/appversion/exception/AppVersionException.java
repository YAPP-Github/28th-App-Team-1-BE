package com.yapp.d14.appversion.exception;

import com.yapp.d14.common.exception.BusinessException;

public class AppVersionException extends BusinessException {

    public AppVersionException(AppVersionErrorCode errorCode) {
        super(errorCode);
    }
}

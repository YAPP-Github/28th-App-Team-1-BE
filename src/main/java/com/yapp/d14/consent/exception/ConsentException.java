package com.yapp.d14.consent.exception;

import com.yapp.d14.common.exception.BusinessException;

public class ConsentException extends BusinessException {

    public ConsentException(ConsentErrorCode errorCode) {
        super(errorCode);
    }
}

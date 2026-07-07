package com.yapp.d14.jd.exception;

import com.yapp.d14.common.exception.BusinessException;

public class JdException extends BusinessException {

    public JdException(JdErrorCode errorCode) {
        super(errorCode);
    }
}

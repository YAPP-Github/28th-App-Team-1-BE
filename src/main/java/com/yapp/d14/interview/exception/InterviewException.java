package com.yapp.d14.interview.exception;

import com.yapp.d14.common.exception.BusinessException;

public class InterviewException extends BusinessException {

    public InterviewException(InterviewErrorCode errorCode) {
        super(errorCode);
    }
}

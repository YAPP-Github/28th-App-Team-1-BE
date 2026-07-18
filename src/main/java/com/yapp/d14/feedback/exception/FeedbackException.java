package com.yapp.d14.feedback.exception;

import com.yapp.d14.common.exception.BusinessException;

public class FeedbackException extends BusinessException {

    public FeedbackException(FeedbackErrorCode errorCode) {
        super(errorCode);
    }
}

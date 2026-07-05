package com.yapp.d14.portfolio.exception;

import com.yapp.d14.common.exception.BusinessException;

public class PortfolioException extends BusinessException {

    public PortfolioException(PortfolioErrorCode errorCode) {
        super(errorCode);
    }
}

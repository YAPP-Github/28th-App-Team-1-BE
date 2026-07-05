package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;

final class PortfolioFileValidator {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_PAGE_COUNT = 30;
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    private PortfolioFileValidator() {
    }

    static void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPE.equals(contentType)) {
            throw new PortfolioException(PortfolioErrorCode.INVALID_FILE_TYPE);
        }
    }

    static void validateFileSize(long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            throw new PortfolioException(PortfolioErrorCode.FILE_TOO_LARGE);
        }
    }

    static void validatePageCount(int pageCount) {
        if (pageCount > MAX_PAGE_COUNT) {
            throw new PortfolioException(PortfolioErrorCode.PAGE_COUNT_EXCEEDED);
        }
    }
}

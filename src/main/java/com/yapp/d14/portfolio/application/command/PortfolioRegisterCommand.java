package com.yapp.d14.portfolio.application.command;

import com.yapp.d14.portfolio.exception.PortfolioErrorCode;
import com.yapp.d14.portfolio.exception.PortfolioException;

import java.util.UUID;

public record PortfolioRegisterCommand(
        UUID userId,
        byte[] fileContent,
        String fileName,
        long declaredFileSize,
        int declaredPageCount,
        String contentType
) {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    public static final int MAX_PAGE_COUNT = 30;
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    public PortfolioRegisterCommand {
        if (!ALLOWED_CONTENT_TYPE.equals(contentType)) {
            throw new PortfolioException(PortfolioErrorCode.INVALID_FILE_TYPE);
        }
        if (fileContent.length > MAX_FILE_SIZE) {
            throw new PortfolioException(PortfolioErrorCode.FILE_TOO_LARGE);
        }
        if (declaredPageCount > MAX_PAGE_COUNT) {
            throw new PortfolioException(PortfolioErrorCode.PAGE_COUNT_EXCEEDED);
        }
    }
}

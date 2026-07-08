package com.yapp.d14.portfolio.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PortfolioErrorCode implements ErrorCode {

    PORTFOLIO_ALREADY_EXISTS(HttpStatus.CONFLICT, "PORTFOLIO_ALREADY_EXISTS", "이미 등록된 포트폴리오가 있어요. 기존 포트폴리오를 삭제한 뒤 새로 올려주세요."),
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND", "포트폴리오를 찾을 수 없어요."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE", "PDF 파일만 올릴 수 있어요"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "파일이 너무 커요. 20MB 이하 PDF로 올려주세요"),
    PAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "PAGE_COUNT_EXCEEDED", "페이지가 너무 많아요. 30페이지 이하 PDF로 올려주세요"),
    INVALID_PDF_FILE(HttpStatus.BAD_REQUEST, "INVALID_PDF_FILE", "파일이 손상된 것 같아요. 파일을 확인하고 다시 시도해 주세요"),
    PDF_PARSING_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "PDF_PARSING_BUSY", "지금 요청이 많아요. 잠시 후 다시 시도해 주세요");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}

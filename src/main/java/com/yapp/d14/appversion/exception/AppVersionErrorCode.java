package com.yapp.d14.appversion.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AppVersionErrorCode implements ErrorCode {

    INVALID_PLATFORM(HttpStatus.BAD_REQUEST, "INVALID_PLATFORM", "지원하지 않는 플랫폼이에요."),
    INVALID_VERSION_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_VERSION_FORMAT", "버전 형식이 올바르지 않아요."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "APP_VERSION_POLICY_NOT_FOUND", "해당 플랫폼의 버전 정책을 찾을 수 없어요.");

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

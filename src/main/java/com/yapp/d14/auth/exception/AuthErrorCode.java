package com.yapp.d14.auth.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIAL(HttpStatus.BAD_REQUEST, "INVALID_CREDENTIAL", "유효하지 않은 인증 정보입니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "SOCIAL_LOGIN_FAILED", "소셜 로그인에 실패했습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "만료된 토큰입니다."),
    LOGIN_EXPIRED(HttpStatus.UNAUTHORIZED, "LOGIN_EXPIRED", "로그인 세션이 만료되었습니다. 다시 로그인해 주세요.");

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

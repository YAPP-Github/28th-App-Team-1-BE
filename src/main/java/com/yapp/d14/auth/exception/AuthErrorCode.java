package com.yapp.d14.auth.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIAL(HttpStatus.BAD_REQUEST, "AUTH_001", "유효하지 않은 인증 정보입니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_002", "소셜 로그인에 실패했습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_003", "존재하지 않는 사용자입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_004", "유효하지 않은 토큰입니다.");

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

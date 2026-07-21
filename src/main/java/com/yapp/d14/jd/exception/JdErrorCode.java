package com.yapp.d14.jd.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JdErrorCode implements ErrorCode {

    INVALID_JD_URL(HttpStatus.BAD_REQUEST, "INVALID_JD_URL", "올바른 URL 형식이 아니에요."),
    JD_VALIDATION_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "JD_VALIDATION_LIMIT_EXCEEDED", "공고 링크는 하루에 5번까지만 입력할 수 있어요");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

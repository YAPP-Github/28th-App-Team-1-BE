package com.yapp.d14.user.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    NAME_ALREADY_TAKEN(HttpStatus.CONFLICT, "NAME_ALREADY_TAKEN", "이미 사용 중인 이름이에요."),
    INVALID_JOB_ROLE(HttpStatus.BAD_REQUEST, "INVALID_JOB_ROLE", "지원하지 않는 직군이에요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

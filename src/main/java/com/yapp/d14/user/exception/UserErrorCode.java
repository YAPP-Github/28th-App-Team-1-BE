package com.yapp.d14.user.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    INVALID_JOB_ROLE(HttpStatus.BAD_REQUEST, "INVALID_JOB_ROLE", "지원하지 않는 직군이에요."),
    // 정지 사유는 내부 용어(NETWORK_DISCONNECT 등)를 드러내지 않는 단일 표준 문구로만 응답한다(PRD Part7 A4).
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "비정상적인 이용 패턴이 반복 확인되어 면접 시작이 제한되었어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

package com.yapp.d14.interview.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum InterviewErrorCode implements ErrorCode {

    INVALID_JOB_ROLE(HttpStatus.BAD_REQUEST, "INVALID_JOB_ROLE", "지원하지 않는 직군이에요."),
    INVALID_CAREER_YEARS(HttpStatus.BAD_REQUEST, "INVALID_CAREER_YEARS", "연차를 다시 확인해 주세요."),
    JD_NOT_VALIDATED(HttpStatus.BAD_REQUEST, "JD_NOT_VALIDATED", "JD 링크를 먼저 검증해 주세요."),
    JD_CONTENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "JD_CONTENT_NOT_FOUND", "JD 링크의 캐시가 만료됐어요. 다시 검증해 주세요."),
    INVALID_JD_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_JD_LENGTH", "JD는 200자 이상 3,000자 이하로 입력해 주세요."),
    INVALID_FREETEXT_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_FREETEXT_LENGTH", "집중 프로젝트 설명은 10자 이상 300자 이하로 입력해 주세요."),
    FREETEXT_NOT_RELEVANT(HttpStatus.BAD_REQUEST, "FREETEXT_NOT_RELEVANT", "입력하신 내용이 포트폴리오와 관련이 적어요."),
    INTERVIEW_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_SESSION_NOT_FOUND", "면접 세션을 찾을 수 없어요.");

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

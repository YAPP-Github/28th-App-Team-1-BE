package com.yapp.d14.feedback.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum FeedbackErrorCode implements ErrorCode {

    // F4 — 공유 설정
    EMPTY_ATTITUDE_AXES(HttpStatus.BAD_REQUEST, "EMPTY_ATTITUDE_AXES", "평가 항목을 최소 1개 선택해 주세요."),
    TOO_MANY_ATTITUDE_AXES(HttpStatus.BAD_REQUEST, "TOO_MANY_ATTITUDE_AXES", "평가 항목은 최대 5개까지 선택할 수 있어요."),
    INVALID_ATTITUDE_AXIS(HttpStatus.BAD_REQUEST, "INVALID_ATTITUDE_AXIS", "지원하지 않는 평가 항목이에요."),
    FEEDBACK_SHARE_ALREADY_EXISTS(HttpStatus.CONFLICT, "FEEDBACK_SHARE_ALREADY_EXISTS", "이미 피드백 요청 링크가 있어요."),
    FEEDBACK_SHARE_NOT_FOUND(HttpStatus.NOT_FOUND, "FEEDBACK_SHARE_NOT_FOUND", "피드백 공유 링크를 찾을 수 없어요."),
    INVALID_SHARE_STATUS(HttpStatus.BAD_REQUEST, "INVALID_SHARE_STATUS", "지원하지 않는 상태 전환이에요."),

    // G4 — 게스트 평가
    FEEDBACK_SHARE_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "FEEDBACK_SHARE_TOKEN_NOT_FOUND", "유효하지 않은 링크예요."),
    FEEDBACK_SHARE_CLOSED(HttpStatus.CONFLICT, "FEEDBACK_SHARE_CLOSED", "지금은 참여할 수 없는 링크예요."),
    FEEDBACK_CAPACITY_FULL(HttpStatus.CONFLICT, "FEEDBACK_CAPACITY_FULL", "이미 4분이 참여했어요."),
    FEEDBACK_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "FEEDBACK_ALREADY_SUBMITTED", "이미 제출하셨어요."),
    INCOMPLETE_RATINGS(HttpStatus.BAD_REQUEST, "INCOMPLETE_RATINGS", "지정된 항목을 모두 평가해 주세요."),
    DUPLICATE_RATING_AXIS(HttpStatus.BAD_REQUEST, "DUPLICATE_RATING_AXIS", "같은 항목을 중복해서 평가할 수 없어요."),
    INVALID_RATING_LEVEL(HttpStatus.BAD_REQUEST, "INVALID_RATING_LEVEL", "척도 값이 올바르지 않아요."),
    MISSING_DEVICE_ID(HttpStatus.BAD_REQUEST, "MISSING_DEVICE_ID", "기기 식별 값이 필요해요.");

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

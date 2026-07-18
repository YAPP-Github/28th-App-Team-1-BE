package com.yapp.d14.interview.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum InterviewErrorCode implements ErrorCode {

    INVALID_JOB_ROLE(HttpStatus.BAD_REQUEST, "INVALID_JOB_ROLE", "지원하지 않는 직군이에요."),
    INVALID_CAREER_YEARS(HttpStatus.BAD_REQUEST, "INVALID_CAREER_YEARS", "연차를 다시 확인해 주세요."),
    JD_NOT_VALIDATED(HttpStatus.BAD_REQUEST, "JD_NOT_VALIDATED", "JD 링크를 먼저 검증해 주세요."),
    JD_URL_AND_TEXT_BOTH_PROVIDED(HttpStatus.BAD_REQUEST, "JD_URL_AND_TEXT_BOTH_PROVIDED", "jdUrl과 jdText는 함께 입력할 수 없어요."),
    JD_CONTENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "JD_CONTENT_NOT_FOUND", "JD 링크의 캐시가 만료됐어요. 다시 검증해 주세요."),
    INVALID_JD_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_JD_LENGTH", "JD는 200자 이상 3,000자 이하로 입력해 주세요."),
    INVALID_FREETEXT_LENGTH(HttpStatus.BAD_REQUEST, "INVALID_FREETEXT_LENGTH", "집중 프로젝트 설명은 10자 이상 300자 이하로 입력해 주세요."),
    FREETEXT_NOT_RELEVANT(HttpStatus.BAD_REQUEST, "FREETEXT_NOT_RELEVANT", "입력하신 내용이 포트폴리오와 관련이 적어요."),
    INTERVIEW_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_SESSION_NOT_FOUND", "면접 세션을 찾을 수 없어요."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "질문을 찾을 수 없어요."),
    ANSWER_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "ANSWER_ALREADY_SUBMITTED", "이미 제출된 답변이에요."),
    SESSION_ALREADY_ENDED(HttpStatus.CONFLICT, "SESSION_ALREADY_ENDED", "이미 종료된 면접 세션이에요."),
    INVALID_PLAYBACK_RANGE(HttpStatus.BAD_REQUEST, "INVALID_PLAYBACK_RANGE", "질문 재생 구간 값이 올바르지 않아요."),
    INVALID_ANSWER_RANGE(HttpStatus.BAD_REQUEST, "INVALID_ANSWER_RANGE", "답변 구간 값이 올바르지 않아요."),
    INVALID_END_TYPE(HttpStatus.BAD_REQUEST, "INVALID_END_TYPE", "지원하지 않는 endType이에요."),
    INVALID_AUDIO_PRESENCE(HttpStatus.BAD_REQUEST, "INVALID_AUDIO_PRESENCE", "endType과 답변 음성 유무가 맞지 않아요."),
    // TODO: turnLevel≥1 일반 매 턴 처리(이슈2 이후)가 구현되기 전까지의 임시 가드
    UNSUPPORTED_TURN_LEVEL(HttpStatus.BAD_REQUEST, "UNSUPPORTED_TURN_LEVEL", "아직 지원하지 않는 turnLevel이에요.");

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

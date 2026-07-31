package com.yapp.d14.consent.exception;

import com.yapp.d14.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ConsentErrorCode implements ErrorCode {

    INVALID_CONSENT_ITEM(HttpStatus.BAD_REQUEST, "INVALID_CONSENT_ITEM", "지원하지 않는 동의 항목이에요."),
    REQUIRED_CONSENT_MISSING(HttpStatus.BAD_REQUEST, "REQUIRED_CONSENT_MISSING", "필수 동의 항목이 누락됐어요."),
    CONSENT_VERSION_MISMATCH(HttpStatus.BAD_REQUEST, "CONSENT_VERSION_MISMATCH", "동의 항목 버전이 최신이 아니에요. 목록을 다시 조회해주세요."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "존재하지 않는 동의 문서예요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

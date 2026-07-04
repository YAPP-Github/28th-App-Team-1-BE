package com.yapp.d14.jd.application.port.in;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JdValidationFailureReason {

    CRAWLING_FAILED("공고 페이지에 접속할 수 없어요. 공고 내용을 직접 붙여넣어 주세요."),
    CONTENT_TOO_SHORT("공고 내용을 충분히 가져오지 못했어요. 공고 내용을 직접 붙여넣어 주세요."),
    EXTRACTION_FAILED("공고 내용을 정리하는 데 실패했어요. 공고 내용을 직접 붙여넣어 주세요.");

    private final String message;
}

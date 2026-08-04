package com.yapp.d14.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 내부 운영 용어다. API 응답에 그대로 실어 보내지 않는다(PRD Part7 A4 용어 표준).
@Getter
@RequiredArgsConstructor
public enum SuspensionReason {

    ABNORMAL_USAGE("비정상 이용 패턴 반복"),
    OPS_MANUAL("운영자 판단");

    private final String label;
}

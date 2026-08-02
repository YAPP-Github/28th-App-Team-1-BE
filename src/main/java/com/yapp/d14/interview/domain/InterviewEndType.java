package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewEndType {

    NORMAL_END("정상 종료"),
    MANUAL_END("수동 종료"),
    HARD_CAP("최대 한도 도달"),
    EARLY_EXIT("중도 이탈"),
    SKIP("답변 건너뜀"),
    STT_RESET("STT 인식 실패");

    private final String label;
}

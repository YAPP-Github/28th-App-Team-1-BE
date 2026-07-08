package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewEndType {

    NORMAL_END("정상 종료"),
    MANUAL_END("수동 종료"),
    QUESTION_EXHAUSTED("질문 소진"),
    HARD_CAP("최대 한도 도달");

    private final String label;
}

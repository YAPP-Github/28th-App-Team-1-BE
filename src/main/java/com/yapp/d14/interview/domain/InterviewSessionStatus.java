package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterviewSessionStatus {

    PREPARING("준비중"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료"),
    ABANDONED("중단"),
    INVALID("무효"),
    PRELOAD_FAILED("실패");

    private final String label;
}

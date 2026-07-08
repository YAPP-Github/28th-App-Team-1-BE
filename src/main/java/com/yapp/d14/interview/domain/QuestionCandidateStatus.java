package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionCandidateStatus {

    OPEN("미사용"),
    USED("사용됨"),
    EXHAUSTED("소진됨"),
    STALE("실효됨");

    private final String label;
}

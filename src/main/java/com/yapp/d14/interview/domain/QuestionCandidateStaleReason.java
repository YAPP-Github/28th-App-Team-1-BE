package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionCandidateStaleReason {

    CONTRADICTED("모순 발생"),
    CORRECTED("자진 정정");

    private final String label;
}

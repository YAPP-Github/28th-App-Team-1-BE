package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TestType {

    DEPTH("깊이"),
    BOUNDARY("경계·규모"),
    CONNECTION("연결"),
    TRADEOFF("대안·우선순위"),
    CONFLICT("갈등"),
    RESILIENCE("성장·복원력");

    private final String label;
}

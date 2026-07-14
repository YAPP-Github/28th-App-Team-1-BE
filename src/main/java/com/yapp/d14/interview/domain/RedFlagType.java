package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RedFlagType {

    FABRICATION(true),
    CONTRADICTION(true),
    PERFECT_NARRATIVE(true),
    BLAME_SHIFTING(false),
    BUZZWORD_SALAD(false);

    private final boolean exposed;
}

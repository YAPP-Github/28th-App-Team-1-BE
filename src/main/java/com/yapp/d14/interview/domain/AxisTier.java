package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AxisTier {

    CORE("핵심"),
    SUPPORT("보조"),
    SKIP("생략");

    private final String label;
}

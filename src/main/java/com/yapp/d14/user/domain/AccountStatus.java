package com.yapp.d14.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountStatus {

    ACTIVE("정상"),
    SUSPENDED("정지");

    private final String label;
}

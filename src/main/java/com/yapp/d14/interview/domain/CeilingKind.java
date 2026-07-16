package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CeilingKind {

    TOPPED_OUT("위로 닿아 멈춤"),
    STUCK("못 올라가서 멈춤");

    private final String label;
}

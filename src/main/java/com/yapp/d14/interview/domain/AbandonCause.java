package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbandonCause {

    NETWORK_DISCONNECT("네트워크 연결 끊김"),
    USER_EXIT("사용자 종료"),
    HOLD_EXPIRED("이용권 보류 만료");

    private final String label;
}

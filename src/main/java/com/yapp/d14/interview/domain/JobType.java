package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobType {

    BACKEND("백엔드"),
    FRONTEND("프론트엔드"),
    IOS("iOS"),
    ANDROID("Android"),
    DATA_ENGINEER("데이터 엔지니어"),
    INFRA_SRE("인프라/SRE");

    private final String label;
}

package com.yapp.d14.job.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Job {

    BACKEND(1, "백엔드"),
    FRONTEND(2, "프론트엔드"),
    IOS(3, "iOS"),
    ANDROID(4, "Android"),
    DATA_ENGINEER(5, "데이터 엔지니어"),
    INFRA_SRE(6, "인프라/SRE");

    private final int id;
    private final String label;
}

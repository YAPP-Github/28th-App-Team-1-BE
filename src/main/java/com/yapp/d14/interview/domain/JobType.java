package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobType {

    // 순서: 깊이, 경계·규모, 연결, 대안·우선순위, 갈등, 성장·복원력 (미들 레벨 기준 가중치, 합계 100)
    BACKEND("백엔드", 25, 20, 10, 20, 10, 15),
    FRONTEND("프론트엔드", 25, 15, 15, 15, 15, 15),
    IOS("iOS", 25, 20, 10, 15, 15, 15),
    ANDROID("Android", 24, 23, 8, 15, 15, 15),
    DATA_ENGINEER("데이터 엔지니어", 25, 20, 12, 18, 10, 15),
    INFRA_SRE("인프라/SRE", 22, 25, 10, 18, 10, 15);

    private final String label;
    private final int depthWeight;
    private final int boundaryWeight;
    private final int connectionWeight;
    private final int tradeoffWeight;
    private final int conflictWeight;
    private final int resilienceWeight;

    public int getBaseWeight(TestType testType) {
        return switch (testType) {
            case DEPTH -> depthWeight;
            case BOUNDARY -> boundaryWeight;
            case CONNECTION -> connectionWeight;
            case TRADEOFF -> tradeoffWeight;
            case CONFLICT -> conflictWeight;
            case RESILIENCE -> resilienceWeight;
        };
    }
}

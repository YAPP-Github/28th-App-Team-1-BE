package com.yapp.d14.interview.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CareerLevel {

    // 순서: 깊이, 경계·규모, 연결, 대안·우선순위, 갈등, 성장·복원력 (연차 델타, 합계 0)
    JUNIOR(4, 0, -3, -3, -3, 5),
    MIDDLE(0, 0, 0, 0, 0, 0),
    SENIOR(-4, 0, 4, 3, 3, -6);

    private final int depthDelta;
    private final int boundaryDelta;
    private final int connectionDelta;
    private final int tradeoffDelta;
    private final int conflictDelta;
    private final int resilienceDelta;

    public static CareerLevel fromYears(int careerYears) {
        if (careerYears <= 2) {
            return JUNIOR;
        }
        if (careerYears <= 7) {
            return MIDDLE;
        }
        return SENIOR;
    }

    public int getDelta(TestType testType) {
        return switch (testType) {
            case DEPTH -> depthDelta;
            case BOUNDARY -> boundaryDelta;
            case CONNECTION -> connectionDelta;
            case TRADEOFF -> tradeoffDelta;
            case CONFLICT -> conflictDelta;
            case RESILIENCE -> resilienceDelta;
        };
    }
}

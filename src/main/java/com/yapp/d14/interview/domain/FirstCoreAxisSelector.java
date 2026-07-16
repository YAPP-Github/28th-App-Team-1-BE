package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 첫 턴(turnLevel=0) 특수 처리 전용: 천장 판별 없이 tier=CORE 중 유효 가중치 1순위 항목을 강제 선택 (설계 문서 4-2장)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FirstCoreAxisSelector {

    public static Optional<TestType> select(List<InterviewAxisPlan> axisPlans, Map<TestType, Integer> weights) {
        return axisPlans.stream()
                .filter(plan -> plan.getTier() == AxisTier.CORE)
                .max(Comparator.comparing(plan -> weights.getOrDefault(plan.getTestType(), 0)))
                .map(InterviewAxisPlan::getTestType);
    }
}

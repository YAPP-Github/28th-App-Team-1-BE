package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NextAxisSelector {

    public static TestType select(
            List<InterviewAxisPlan> axisPlans,
            Map<TestType, Integer> weights,
            TestType currentAxis,
            boolean ceilingReached,
            boolean hasRedFlag,
            boolean isUnusuallySpecific
    ) {
        InterviewAxisPlan currentPlan = findPlan(axisPlans, currentAxis);
        boolean budgetExhausted = currentPlan.getUsedCount() >= currentPlan.getBudget();

        // 축별 질문 예산(budget)은 하드 상한이다. 레드플래그·유독 구체적인 답변이면 천장에 닿았어도
        // 예산이 남아 있는 한 축을 유지해 한 번 더 캐묻지만, 예산을 소진하면 신호가 있어도 다음 축으로 넘긴다.
        // 한 축을 무한정 파고들어 나머지 핵심 축을 못 캐고 종합점수 미산출(분석 부족)로 빠지는 걸 막기 위해서다.
        if ((hasRedFlag || isUnusuallySpecific) && !budgetExhausted) {
            return currentAxis;
        }
        if (!ceilingReached && !budgetExhausted) {
            return currentAxis;
        }
        return nextAxisByTier(axisPlans, weights, currentAxis);
    }

    private static TestType nextAxisByTier(List<InterviewAxisPlan> axisPlans, Map<TestType, Integer> weights, TestType currentAxis) {
        return pickByTier(axisPlans, weights, AxisTier.CORE, currentAxis)
                .or(() -> pickByTier(axisPlans, weights, AxisTier.SUPPORT, currentAxis))
                .orElse(currentAxis);
    }

    private static Optional<TestType> pickByTier(
            List<InterviewAxisPlan> axisPlans, Map<TestType, Integer> weights, AxisTier tier, TestType currentAxis
    ) {
        return axisPlans.stream()
                .filter(plan -> plan.getTier() == tier)
                .filter(plan -> !plan.isCompleted())
                .filter(plan -> plan.getTestType() != currentAxis)
                .max(Comparator
                        .comparing((InterviewAxisPlan plan) -> weights.getOrDefault(plan.getTestType(), 0))
                        .thenComparing(plan -> plan.getTestType().name()))
                .map(InterviewAxisPlan::getTestType);
    }

    private static InterviewAxisPlan findPlan(List<InterviewAxisPlan> axisPlans, TestType testType) {
        return axisPlans.stream()
                .filter(plan -> plan.getTestType() == testType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("axis_plan을 찾을 수 없어요. testType=" + testType));
    }
}

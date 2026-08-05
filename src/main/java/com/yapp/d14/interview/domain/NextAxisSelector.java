package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NextAxisSelector {

    // 레드플래그·유독 구체적인 답변이면 예산을 넘겨 "한 번 더" 캐묻지만(PRD Part2 §4.4-4·§4.6),
    // 그것도 넘을 수 없는 절대 상한이다. budget + 1(핵심 축이면 3→최대 4, §4.5 "항목당 4개까지")을 넘으면
    // 신호가 있어도 다음 축으로 넘긴다 — 한 축을 무한정 파고들어 나머지 핵심 축을 못 캐고
    // 종합점수 미산출(§6.7 분석 부족)로 빠지는 걸 막기 위해서다.
    private static final int OVERRIDE_EXTRA_BUDGET = 1;

    public static TestType select(
            List<InterviewAxisPlan> axisPlans,
            Map<TestType, Integer> weights,
            TestType currentAxis,
            boolean ceilingReached,
            boolean hasRedFlag,
            boolean isUnusuallySpecific
    ) {
        InterviewAxisPlan currentPlan = findPlan(axisPlans, currentAxis);
        int usedCount = currentPlan.getUsedCount();

        boolean overrideCapReached = usedCount >= currentPlan.getBudget() + OVERRIDE_EXTRA_BUDGET;
        if ((hasRedFlag || isUnusuallySpecific) && !overrideCapReached) {
            return currentAxis;
        }

        boolean budgetExhausted = usedCount >= currentPlan.getBudget();
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

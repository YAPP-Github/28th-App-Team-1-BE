package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SelectNextAxis {

    public static TestType select(
            List<InterviewAxisPlan> axisPlans,
            Map<TestType, Integer> weights,
            TestType currentAxis,
            boolean ceilingReached,
            boolean hasRedFlag,
            boolean isUnusuallySpecific
    ) {
        if (hasRedFlag || isUnusuallySpecific) {
            return currentAxis;
        }
        InterviewAxisPlan currentPlan = findPlan(axisPlans, currentAxis);
        boolean budgetExhausted = currentPlan.getUsedCount() >= currentPlan.getBudget();
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
                .max(Comparator.comparing(plan -> weights.getOrDefault(plan.getTestType(), 0)))
                .map(InterviewAxisPlan::getTestType);
    }

    private static InterviewAxisPlan findPlan(List<InterviewAxisPlan> axisPlans, TestType testType) {
        return axisPlans.stream()
                .filter(plan -> plan.getTestType() == testType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("axis_plan을 찾을 수 없어요. testType=" + testType));
    }
}

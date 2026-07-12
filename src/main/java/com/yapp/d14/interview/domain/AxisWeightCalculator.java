package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AxisWeightCalculator {

    private static final int TOTAL_WEIGHT = 100;
    private static final int CORE_COUNT = 3;
    private static final int CORE_BUDGET = 3;
    private static final int SUPPORT_COUNT = 1;
    private static final int SUPPORT_BUDGET = 1;

    public record AxisAssignment(AxisTier tier, int budget) {
    }

    public static Map<TestType, Integer> compute(JobType jobRole, int careerYears) {
        CareerLevel careerLevel = CareerLevel.fromYears(careerYears);

        Map<TestType, Integer> raw = new EnumMap<>(TestType.class);
        for (TestType testType : TestType.values()) {
            int value = jobRole.getBaseWeight(testType) + careerLevel.getDelta(testType);
            raw.put(testType, Math.max(value, 0));
        }

        int sum = raw.values().stream().mapToInt(Integer::intValue).sum();
        if (sum == TOTAL_WEIGHT || sum == 0) {
            return raw;
        }

        Map<TestType, Integer> normalized = new EnumMap<>(TestType.class);
        for (Map.Entry<TestType, Integer> entry : raw.entrySet()) {
            normalized.put(entry.getKey(), Math.round(entry.getValue() * TOTAL_WEIGHT / (float) sum));
        }
        return normalized;
    }

    public static Map<TestType, AxisAssignment> assignTierAndBudget(Map<TestType, Integer> weights) {
        List<TestType> ranked = weights.entrySet().stream()
                .sorted(Comparator.<Map.Entry<TestType, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .map(Map.Entry::getKey)
                .toList();

        Map<TestType, AxisAssignment> assignments = new EnumMap<>(TestType.class);
        for (int i = 0; i < ranked.size(); i++) {
            TestType testType = ranked.get(i);
            if (i < CORE_COUNT) {
                assignments.put(testType, new AxisAssignment(AxisTier.CORE, CORE_BUDGET));
            } else if (i < CORE_COUNT + SUPPORT_COUNT) {
                assignments.put(testType, new AxisAssignment(AxisTier.SUPPORT, SUPPORT_BUDGET));
            } else {
                assignments.put(testType, new AxisAssignment(AxisTier.SKIP, 0));
            }
        }
        return assignments;
    }
}

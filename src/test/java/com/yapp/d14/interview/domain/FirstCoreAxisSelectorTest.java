package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FirstCoreAxisSelectorTest {

    private static InterviewAxisPlan plan(TestType testType, AxisTier tier) {
        return InterviewAxisPlan.create(1L, testType, tier, 3);
    }

    @Test
    void CORE_tier가_없으면_빈_값을_반환한다() {
        List<InterviewAxisPlan> axisPlans = List.of(plan(TestType.DEPTH, AxisTier.SUPPORT));
        Map<TestType, Integer> weights = new EnumMap<>(Map.of(TestType.DEPTH, 25));

        Optional<TestType> selected = FirstCoreAxisSelector.select(axisPlans, weights);

        assertThat(selected).isEmpty();
    }

    @Test
    void CORE_tier_중_유효_가중치가_가장_높은_항목을_선택한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE),
                plan(TestType.TRADEOFF, AxisTier.CORE),
                plan(TestType.BOUNDARY, AxisTier.CORE),
                plan(TestType.CONNECTION, AxisTier.SUPPORT)
        );
        Map<TestType, Integer> weights = new EnumMap<>(Map.of(
                TestType.DEPTH, 21,
                TestType.TRADEOFF, 23,
                TestType.BOUNDARY, 20,
                TestType.CONNECTION, 14
        ));

        Optional<TestType> selected = FirstCoreAxisSelector.select(axisPlans, weights);

        assertThat(selected).contains(TestType.TRADEOFF);
    }
}

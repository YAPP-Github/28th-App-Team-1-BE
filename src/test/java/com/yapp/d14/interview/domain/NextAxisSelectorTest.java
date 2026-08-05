package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NextAxisSelectorTest {

    private static InterviewAxisPlan plan(TestType testType, AxisTier tier, int budget, int usedCount, boolean completed) {
        InterviewAxisPlan plan = InterviewAxisPlan.create(1L, testType, tier, budget);
        for (int i = 0; i < usedCount; i++) {
            plan.incrementUsedCount();
        }
        if (completed) {
            plan.markCompleted();
        }
        return plan;
    }

    private static final Map<TestType, Integer> WEIGHTS = new EnumMap<>(Map.of(
            TestType.DEPTH, 23,
            TestType.BOUNDARY, 21,
            TestType.TRADEOFF, 20,
            TestType.CONNECTION, 14,
            TestType.CONFLICT, 13,
            TestType.RESILIENCE, 9
    ));

    @Test
    void 천장에도_안_닿고_예산도_남았으면_현재_axis를_유지한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 1, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 0, false),
                plan(TestType.TRADEOFF, AxisTier.CORE, 3, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, false, false, false);

        assertThat(next).isEqualTo(TestType.DEPTH);
    }

    @Test
    void 위험_신호가_있으면_천장_예산과_무관하게_현재_axis를_유지한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 3, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, true, true, false);

        assertThat(next).isEqualTo(TestType.DEPTH);
    }

    @Test
    void 유독_구체적인_답변이면_천장_예산과_무관하게_현재_axis를_유지한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 3, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, true, false, true);

        assertThat(next).isEqualTo(TestType.DEPTH);
    }

    @Test
    void 위험_신호가_있어도_예산_상한_한번더까지_넘겼으면_다음_CORE_axis로_전환한다() {
        // budget=3 + OVERRIDE_EXTRA_BUDGET(1) = 4. usedCount가 4에 닿으면 신호가 있어도 더는 유지하지 않는다.
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 4, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 0, false),
                plan(TestType.TRADEOFF, AxisTier.CORE, 3, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, false, true, false);

        assertThat(next).isEqualTo(TestType.BOUNDARY);
    }

    @Test
    void 유독_구체적인_답변이어도_예산_상한_한번더까지_넘겼으면_다음_CORE_axis로_전환한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 4, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 0, false),
                plan(TestType.TRADEOFF, AxisTier.CORE, 3, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, false, false, true);

        assertThat(next).isEqualTo(TestType.BOUNDARY);
    }

    @Test
    void 천장에_닿으면_예산이_남아도_다음_CORE_axis로_전환한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 1, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 0, false),
                plan(TestType.TRADEOFF, AxisTier.CORE, 3, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, true, false, false);

        assertThat(next).isEqualTo(TestType.BOUNDARY);
    }

    @Test
    void 예산이_소진되면_천장에_안_닿아도_다음_CORE_axis로_전환한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 3, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 0, false),
                plan(TestType.TRADEOFF, AxisTier.CORE, 3, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, false, false, false);

        assertThat(next).isEqualTo(TestType.BOUNDARY);
    }

    @Test
    void 전환_시_이미_완료된_CORE_axis는_건너뛰고_다음으로_넘어간다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 3, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 3, true),
                plan(TestType.TRADEOFF, AxisTier.CORE, 3, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, false, false, false);

        assertThat(next).isEqualTo(TestType.TRADEOFF);
    }

    @Test
    void CORE에_남은_항목이_없으면_SUPPORT로_전환한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 3, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 3, true),
                plan(TestType.TRADEOFF, AxisTier.CORE, 3, 3, true),
                plan(TestType.CONNECTION, AxisTier.SUPPORT, 2, 0, false)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, false, false, false);

        assertThat(next).isEqualTo(TestType.CONNECTION);
    }

    @Test
    void CORE_SUPPORT_모두_남지_않으면_현재_axis를_유지한다() {
        List<InterviewAxisPlan> axisPlans = List.of(
                plan(TestType.DEPTH, AxisTier.CORE, 3, 3, false),
                plan(TestType.BOUNDARY, AxisTier.CORE, 3, 3, true)
        );

        TestType next = NextAxisSelector.select(axisPlans, WEIGHTS, TestType.DEPTH, true, false, false);

        assertThat(next).isEqualTo(TestType.DEPTH);
    }
}

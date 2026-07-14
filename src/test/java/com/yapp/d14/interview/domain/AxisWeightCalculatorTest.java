package com.yapp.d14.interview.domain;

import com.yapp.d14.interview.domain.AxisWeightCalculator.AxisAssignment;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AxisWeightCalculatorTest {

    @Test
    void MIDDLE_레벨은_직군_기준_가중치를_그대로_사용하고_합은_100이다() {
        for (JobType jobType : JobType.values()) {
            Map<TestType, Integer> weights = AxisWeightCalculator.compute(jobType, 5);

            int sum = weights.values().stream().mapToInt(Integer::intValue).sum();
            assertThat(sum).as("%s 가중치 합", jobType).isEqualTo(100);
            for (TestType testType : TestType.values()) {
                assertThat(weights.get(testType)).isEqualTo(jobType.getBaseWeight(testType));
            }
        }
    }

    @Test
    void 시니어_백엔드는_문서_예시와_동일한_유효_가중치를_계산한다() {
        Map<TestType, Integer> weights = AxisWeightCalculator.compute(JobType.BACKEND, 8);

        assertThat(weights)
                .containsEntry(TestType.DEPTH, 21)
                .containsEntry(TestType.BOUNDARY, 20)
                .containsEntry(TestType.CONNECTION, 14)
                .containsEntry(TestType.TRADEOFF, 23)
                .containsEntry(TestType.CONFLICT, 13)
                .containsEntry(TestType.RESILIENCE, 9);
    }

    @Test
    void 모든_직군_레벨_조합에서_가중치_합은_100이다() {
        for (JobType jobType : JobType.values()) {
            for (int careerYears : new int[]{0, 2, 3, 7, 8, 15}) {
                Map<TestType, Integer> weights = AxisWeightCalculator.compute(jobType, careerYears);

                int sum = weights.values().stream().mapToInt(Integer::intValue).sum();
                assertThat(sum).as("%s, %d년", jobType, careerYears).isEqualTo(100);
            }
        }
    }

    @Test
    void 시니어_백엔드는_상위_3개가_CORE_4번째가_SUPPORT_나머지가_SKIP이다() {
        Map<TestType, Integer> weights = AxisWeightCalculator.compute(JobType.BACKEND, 8);

        Map<TestType, AxisAssignment> assignments = AxisWeightCalculator.assignTierAndBudget(weights);

        assertThat(assignments.get(TestType.TRADEOFF)).isEqualTo(new AxisAssignment(AxisTier.CORE, 3));
        assertThat(assignments.get(TestType.DEPTH)).isEqualTo(new AxisAssignment(AxisTier.CORE, 3));
        assertThat(assignments.get(TestType.BOUNDARY)).isEqualTo(new AxisAssignment(AxisTier.CORE, 3));
        assertThat(assignments.get(TestType.CONNECTION)).isEqualTo(new AxisAssignment(AxisTier.SUPPORT, 1));
        assertThat(assignments.get(TestType.CONFLICT)).isEqualTo(new AxisAssignment(AxisTier.SKIP, 0));
        assertThat(assignments.get(TestType.RESILIENCE)).isEqualTo(new AxisAssignment(AxisTier.SKIP, 0));
    }

    @Test
    void tier_배정은_항상_CORE_3개_SUPPORT_1개_SKIP_2개다() {
        for (JobType jobType : JobType.values()) {
            Map<TestType, Integer> weights = AxisWeightCalculator.compute(jobType, 3);
            Map<TestType, AxisAssignment> assignments = AxisWeightCalculator.assignTierAndBudget(weights);

            long coreCount = assignments.values().stream().filter(a -> a.tier() == AxisTier.CORE).count();
            long supportCount = assignments.values().stream().filter(a -> a.tier() == AxisTier.SUPPORT).count();
            long skipCount = assignments.values().stream().filter(a -> a.tier() == AxisTier.SKIP).count();

            assertThat(coreCount).as("%s CORE 개수", jobType).isEqualTo(3);
            assertThat(supportCount).as("%s SUPPORT 개수", jobType).isEqualTo(1);
            assertThat(skipCount).as("%s SKIP 개수", jobType).isEqualTo(2);
        }
    }
}

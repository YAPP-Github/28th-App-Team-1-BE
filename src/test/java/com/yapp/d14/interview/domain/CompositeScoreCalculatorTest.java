package com.yapp.d14.interview.domain;

import com.yapp.d14.interview.domain.CompositeScoreCalculator.Result;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeScoreCalculatorTest {

    private static AxisEvaluation score(TestType testType, int score) {
        return AxisEvaluation.create(1L, testType, score, ResolutionLevel.NORMAL, null, List.of(), "rationale");
    }

    @Test
    void 원문_예시_11년차_시니어_PM은_종합점수_3점80_Strong_Hire다() {
        Map<TestType, Integer> weights = new EnumMap<>(Map.of(
                TestType.DEPTH, 6,
                TestType.BOUNDARY, 10,
                TestType.CONNECTION, 29,
                TestType.TRADEOFF, 25,
                TestType.CONFLICT, 26,
                TestType.RESILIENCE, 4
        ));
        List<AxisEvaluation> axisScores = List.of(
                score(TestType.DEPTH, 3),
                score(TestType.BOUNDARY, 3),
                score(TestType.CONNECTION, 4),
                score(TestType.TRADEOFF, 4),
                score(TestType.CONFLICT, 4),
                score(TestType.RESILIENCE, 3)
        );

        Optional<Result> result = CompositeScoreCalculator.compute(axisScores, weights, Set.of(), false);

        assertThat(result).isPresent();
        assertThat(result.get().compositeScore()).isEqualTo(3.80);
        assertThat(result.get().grade()).isEqualTo(InternalGrade.STRONG_HIRE);
    }

    @Test
    void 원문_예시_2년차_주니어_백엔드는_종합점수_3점18_Hire다() {
        Map<TestType, Integer> weights = AxisWeightCalculator.compute(JobType.BACKEND, 2);
        List<AxisEvaluation> axisScores = List.of(
                score(TestType.DEPTH, 4),
                score(TestType.BOUNDARY, 3),
                score(TestType.CONNECTION, 2),
                score(TestType.TRADEOFF, 2),
                score(TestType.CONFLICT, 2),
                score(TestType.RESILIENCE, 4)
        );

        Optional<Result> result = CompositeScoreCalculator.compute(axisScores, weights, Set.of(), false);

        assertThat(result).isPresent();
        assertThat(result.get().compositeScore()).isEqualTo(3.18);
        assertThat(result.get().grade()).isEqualTo(InternalGrade.HIRE);
    }

    @Test
    void 원문_예시_6년차_미들_iOS는_종합점수_2점85_Lean이다() {
        Map<TestType, Integer> weights = AxisWeightCalculator.compute(JobType.IOS, 6);
        List<AxisEvaluation> axisScores = List.of(
                score(TestType.DEPTH, 4),
                score(TestType.BOUNDARY, 3),
                score(TestType.CONNECTION, 2),
                score(TestType.TRADEOFF, 3),
                score(TestType.CONFLICT, 2),
                score(TestType.RESILIENCE, 2)
        );

        Optional<Result> result = CompositeScoreCalculator.compute(axisScores, weights, Set.of(), false);

        assertThat(result).isPresent();
        assertThat(result.get().compositeScore()).isEqualTo(2.85);
        assertThat(result.get().grade()).isEqualTo(InternalGrade.LEAN);
    }

    @Test
    void 안_캔_항목은_합산에서_제외된다() {
        Map<TestType, Integer> weights = AxisWeightCalculator.compute(JobType.BACKEND, 5);
        List<AxisEvaluation> axisScores = List.of(
                score(TestType.DEPTH, 4),
                score(TestType.BOUNDARY, 4),
                score(TestType.TRADEOFF, 4)
        );

        Optional<Result> result = CompositeScoreCalculator.compute(axisScores, weights, Set.of(), false);

        int expectedWeightSum = weights.get(TestType.DEPTH) + weights.get(TestType.BOUNDARY) + weights.get(TestType.TRADEOFF);
        assertThat(result).isPresent();
        assertThat(result.get().compositeScore()).isEqualTo(4.0);
        assertThat(expectedWeightSum).isLessThan(100);
    }

    @Test
    void 핵심_항목이_채점되지_않으면_종합점수는_미산출이다() {
        Map<TestType, Integer> weights = AxisWeightCalculator.compute(JobType.BACKEND, 5);
        List<AxisEvaluation> axisScores = List.of(score(TestType.RESILIENCE, 3));

        Optional<Result> result = CompositeScoreCalculator.compute(
                axisScores, weights, Set.of(TestType.DEPTH, TestType.BOUNDARY, TestType.TRADEOFF), false
        );

        assertThat(result).isEmpty();
    }

    @Test
    void 레드플래그_cap이_적용된_axis는_capped_점수로_계산된다() {
        Map<TestType, Integer> weights = AxisWeightCalculator.compute(JobType.BACKEND, 5);
        AxisEvaluation capped = score(TestType.CONFLICT, 4).withAppliedCap(2);
        List<AxisEvaluation> axisScores = List.of(
                score(TestType.DEPTH, 4),
                capped
        );

        Optional<Result> result = CompositeScoreCalculator.compute(axisScores, weights, Set.of(), false);

        int depthWeight = weights.get(TestType.DEPTH);
        int conflictWeight = weights.get(TestType.CONFLICT);
        double expected = Math.round((4.0 * depthWeight + 2.0 * conflictWeight) * 100.0 / (depthWeight + conflictWeight)) / 100.0;
        assertThat(result).isPresent();
        assertThat(result.get().compositeScore()).isEqualTo(expected);
    }

    @Test
    void 날조_knockout이_발생하면_점수와_무관하게_NO_이하로_강등된다() {
        Map<TestType, Integer> weights = new EnumMap<>(Map.of(
                TestType.DEPTH, 6,
                TestType.BOUNDARY, 10,
                TestType.CONNECTION, 29,
                TestType.TRADEOFF, 25,
                TestType.CONFLICT, 26,
                TestType.RESILIENCE, 4
        ));
        List<AxisEvaluation> axisScores = List.of(
                score(TestType.DEPTH, 4),
                score(TestType.BOUNDARY, 4),
                score(TestType.CONNECTION, 4),
                score(TestType.TRADEOFF, 4),
                score(TestType.CONFLICT, 4),
                score(TestType.RESILIENCE, 4)
        );

        Optional<Result> result = CompositeScoreCalculator.compute(axisScores, weights, Set.of(), true);

        assertThat(result).isPresent();
        assertThat(result.get().compositeScore()).isEqualTo(4.0);
        assertThat(result.get().grade()).isEqualTo(InternalGrade.NO);
    }

    @Test
    void knockout이_이미_NO_이하인_경우_등급을_더_낮추지_않는다() {
        Map<TestType, Integer> weights = AxisWeightCalculator.compute(JobType.BACKEND, 5);
        List<AxisEvaluation> axisScores = List.of(score(TestType.DEPTH, 1));

        Optional<Result> result = CompositeScoreCalculator.compute(axisScores, weights, Set.of(), true);

        assertThat(result).isPresent();
        assertThat(result.get().grade()).isEqualTo(InternalGrade.STRONG_NO);
    }
}

package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompositeScoreCalculator {

    public record Result(double compositeScore, InternalGrade grade) {
    }

    public static Optional<Result> compute(
            List<AxisEvaluation> axisScores,
            Map<TestType, Integer> weights,
            Set<TestType> coreAxes,
            boolean knockoutTriggered
    ) {
        Set<TestType> scoredAxes = axisScores.stream()
                .map(AxisEvaluation::getTestType)
                .collect(Collectors.toSet());
        if (!scoredAxes.containsAll(coreAxes)) {
            return Optional.empty();
        }

        long weightedSum = 0;
        long weightSum = 0;
        for (AxisEvaluation axisScore : axisScores) {
            int weight = weights.getOrDefault(axisScore.getTestType(), 0);
            weightedSum += (long) axisScore.effectiveScore() * weight;
            weightSum += weight;
        }
        if (weightSum == 0) {
            return Optional.empty();
        }

        double compositeScore = Math.round(weightedSum * 100.0 / weightSum) / 100.0;
        InternalGrade grade = InternalGrade.fromScore(compositeScore);
        if (knockoutTriggered) {
            grade = grade.capAtNo();
        }
        return Optional.of(new Result(compositeScore, grade));
    }
}

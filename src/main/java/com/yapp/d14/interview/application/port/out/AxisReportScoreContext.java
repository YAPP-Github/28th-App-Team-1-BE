package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.TestType;

import java.util.List;

public record AxisReportScoreContext(
        List<AxisTurnGroup> axisTurnGroups
) {

    public record AxisTurnGroup(
            TestType testType,
            List<Turn> turns
    ) {
    }

    public record Turn(
            String questionContent,
            String answerText,
            boolean skipped,
            Float answerStartSec,
            Float answerEndSec
    ) {
    }
}

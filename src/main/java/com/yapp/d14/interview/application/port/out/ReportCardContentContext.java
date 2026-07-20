package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.ResolutionLowReason;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;

public record ReportCardContentContext(
        List<AxisCardInput> axisCards
) {

    public record AxisCardInput(
            TestType testType,
            List<Turn> turns,
            String scoringRationale,
            ResolutionLevel resolutionLevel,
            ResolutionLowReason resolutionLowReason
    ) {
    }

    public record Turn(
            Long questionId,
            int depthLevel,
            String questionContent,
            String answerText,
            boolean skipped,
            Float answerStartSec,
            Float answerEndSec
    ) {
    }
}

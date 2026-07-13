package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.TestType;

import java.util.List;

public record HeadlineContext(
        boolean severeRedFlagPresent,
        List<AxisTopic> axisTopics
) {

    public record AxisTopic(
            TestType testType,
            String scoringRationale,
            ResolutionLevel resolutionLevel
    ) {
    }
}

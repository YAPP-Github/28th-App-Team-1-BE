package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.ResolutionLevel;
import com.yapp.d14.interview.domain.ResolutionLowReason;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TimeRange;

import java.util.List;

public record AxisScoreDraft(
        TestType testType,
        int score,
        ResolutionLevel resolutionLevel,
        ResolutionLowReason resolutionLowReason,
        List<TimeRange> evidenceTimestamps,
        String rationale
) {
}

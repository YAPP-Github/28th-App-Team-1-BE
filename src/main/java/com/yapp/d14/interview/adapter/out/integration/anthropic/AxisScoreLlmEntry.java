package com.yapp.d14.interview.adapter.out.integration.anthropic;

import java.util.List;

record AxisScoreLlmEntry(
        String axis,
        int score,
        String resolutionLevel,
        String resolutionLowReason,
        List<TimeRangeLlmEntry> evidenceTimestamps,
        String rationale
) {

    record TimeRangeLlmEntry(Float startSec, Float endSec) {
    }
}

package com.yapp.d14.interview.adapter.out.integration.anthropic;

import java.util.List;

record RedFlagVerdictLlmEntry(
        String type,
        String affectedTestType,
        Integer capValue,
        boolean knockout,
        List<TimeRangeLlmEntry> evidenceTimestamps,
        List<Integer> relatedTurns,
        String rationale
) {

    record TimeRangeLlmEntry(Float startSec, Float endSec) {
    }
}

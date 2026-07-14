package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.RedFlagType;
import com.yapp.d14.interview.domain.TestType;
import com.yapp.d14.interview.domain.TimeRange;

import java.util.List;

public record RedFlagVerdict(
        RedFlagType type,
        TestType affectedTestType,
        Integer capValue,
        boolean knockout,
        List<TimeRange> evidenceTimestamps,
        String rationale
) {
}

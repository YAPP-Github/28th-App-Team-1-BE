package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;

public record ProbeCandidateDraft(
        TestType testType,
        TestType secondaryTestType,
        String probeText,
        String echoQuote,
        String jdMatch,
        QuestionCandidateStrength strength
) {
}

package com.yapp.d14.interview.adapter.out.integration.anthropic;

record ProbeCandidateLlmEntry(
        String axis,
        String secondaryAxis,
        String probeText,
        String echoQuote,
        String jdMatch,
        String strength,
        String principleUsed
) {
}

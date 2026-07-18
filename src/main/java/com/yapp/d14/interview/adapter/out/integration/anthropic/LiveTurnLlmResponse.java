package com.yapp.d14.interview.adapter.out.integration.anthropic;

import java.util.List;

record LiveTurnLlmResponse(
        List<ProbeCandidateLlmEntry> newProbes,
        CeilingLlmEntry ceiling,
        List<StaleUpdateLlmEntry> staleUpdates
) {
}

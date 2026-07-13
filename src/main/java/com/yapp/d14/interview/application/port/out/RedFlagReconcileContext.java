package com.yapp.d14.interview.application.port.out;

import com.yapp.d14.interview.domain.TestType;

import java.util.List;

public record RedFlagReconcileContext(
        List<PortfolioCandidate> portfolioCandidates,
        List<ContradictionCandidate> contradictionCandidates,
        List<Turn> turns
) {

    public record PortfolioCandidate(
            TestType testType,
            String probeText,
            String echoQuote
    ) {
    }

    public record ContradictionCandidate(
            TestType testType,
            String echoQuote,
            String probeText,
            Integer originTurnNumber,
            Integer contradictingTurnNumber
    ) {
    }

    public record Turn(
            int turnNumber,
            TestType testType,
            String questionContent,
            String answerText,
            boolean skipped,
            Float answerStartSec,
            Float answerEndSec
    ) {
    }
}

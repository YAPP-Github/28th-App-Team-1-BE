package com.yapp.d14.interview.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NextProbeSelector {

    private static final Comparator<QuestionCandidate> PRIORITY = Comparator
            .comparing((QuestionCandidate candidate) -> candidate.getJdMatch() != null)
            .thenComparing(candidate -> strengthRank(candidate.getStrength()));

    public static Optional<QuestionCandidate> select(List<QuestionCandidate> openCandidates) {
        return openCandidates.stream().max(PRIORITY);
    }

    private static int strengthRank(QuestionCandidateStrength strength) {
        return switch (strength) {
            case HIGH -> 2;
            case MID -> 1;
            case LOW -> 0;
        };
    }
}

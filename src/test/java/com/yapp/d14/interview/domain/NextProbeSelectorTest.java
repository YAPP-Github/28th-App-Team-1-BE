package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NextProbeSelectorTest {

    private static QuestionCandidate candidate(String jdMatch, QuestionCandidateStrength strength) {
        return QuestionCandidate.create(
                1L,
                QuestionCandidateSource.PORTFOLIO,
                "ref",
                TestType.DEPTH,
                null,
                "probe",
                "echo",
                jdMatch,
                strength,
                null
        );
    }

    @Test
    void 후보가_없으면_빈_값을_반환한다() {
        Optional<QuestionCandidate> selected = NextProbeSelector.select(List.of());

        assertThat(selected).isEmpty();
    }

    @Test
    void jd_match가_있는_후보를_없는_후보보다_우선한다() {
        QuestionCandidate withJdMatch = candidate("Kafka", QuestionCandidateStrength.LOW);
        QuestionCandidate withoutJdMatch = candidate(null, QuestionCandidateStrength.HIGH);

        Optional<QuestionCandidate> selected = NextProbeSelector.select(List.of(withoutJdMatch, withJdMatch));

        assertThat(selected).contains(withJdMatch);
    }

    @Test
    void jd_match_동률이면_strength가_높은_후보를_우선한다() {
        QuestionCandidate high = candidate("Kafka", QuestionCandidateStrength.HIGH);
        QuestionCandidate mid = candidate("Kafka", QuestionCandidateStrength.MID);
        QuestionCandidate low = candidate("Kafka", QuestionCandidateStrength.LOW);

        Optional<QuestionCandidate> selected = NextProbeSelector.select(List.of(low, high, mid));

        assertThat(selected).contains(high);
    }
}

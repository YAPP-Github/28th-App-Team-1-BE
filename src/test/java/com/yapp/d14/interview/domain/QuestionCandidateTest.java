package com.yapp.d14.interview.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionCandidateTest {

    private static QuestionCandidate create() {
        return QuestionCandidate.create(
                1L, QuestionCandidateSource.ANSWER, "턴 1", TestType.DEPTH, null,
                "probe", "echo", null, QuestionCandidateStrength.HIGH, "P3"
        );
    }

    @Test
    void 생성_시_principleUsed가_그대로_저장된다() {
        QuestionCandidate candidate = create();

        assertThat(candidate.getPrincipleUsed()).isEqualTo("P3");
        assertThat(candidate.getStatus()).isEqualTo(QuestionCandidateStatus.OPEN);
    }

    @Test
    void markStale_호출_시_상태와_사유_모순턴이_기록된다() {
        QuestionCandidate candidate = create();

        candidate.markStale(QuestionCandidateStaleReason.CONTRADICTED, 5);

        assertThat(candidate.getStatus()).isEqualTo(QuestionCandidateStatus.STALE);
        assertThat(candidate.getStaleReason()).isEqualTo(QuestionCandidateStaleReason.CONTRADICTED);
        assertThat(candidate.getContradictingTurnNumber()).isEqualTo(5);
    }

    @Test
    void corrected_사유는_모순턴만_기록하고_레드플래그_의미를_갖지_않는다() {
        QuestionCandidate candidate = create();

        candidate.markStale(QuestionCandidateStaleReason.CORRECTED, 3);

        assertThat(candidate.getStatus()).isEqualTo(QuestionCandidateStatus.STALE);
        assertThat(candidate.getStaleReason()).isEqualTo(QuestionCandidateStaleReason.CORRECTED);
    }

    @Test
    void markExhausted_호출_시_상태가_EXHAUSTED로_바뀐다() {
        QuestionCandidate candidate = create();

        candidate.markExhausted();

        assertThat(candidate.getStatus()).isEqualTo(QuestionCandidateStatus.EXHAUSTED);
    }
}

package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.application.port.out.StaleProbeUpdate;
import com.yapp.d14.interview.domain.QuestionCandidate;
import com.yapp.d14.interview.domain.QuestionCandidateSource;
import com.yapp.d14.interview.domain.QuestionCandidateStaleReason;
import com.yapp.d14.interview.domain.QuestionCandidateStatus;
import com.yapp.d14.interview.domain.QuestionCandidateStrength;
import com.yapp.d14.interview.domain.TestType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// LLM 응답(staleUpdates)이 스키마 밖 reason 값을 반환해도 답변 제출 전체가 죽지 않고
// 해당 항목만 무시하는지 고정한다(#122).
class AnthropicLiveTurnAnalyzerAdapterStaleUpdateMappingTest {

    private static QuestionCandidate openProbe(Long id) {
        return QuestionCandidate.of(
                id, 1L, QuestionCandidateSource.ANSWER, null,
                TestType.DEPTH, null, "probeText", "echoQuote", null,
                QuestionCandidateStrength.HIGH, QuestionCandidateStatus.OPEN, null,
                null, null, LocalDateTime.now(), null
        );
    }

    @Test
    void reason이_contradicted_corrected면_정상_매핑한다() {
        List<QuestionCandidate> openProbes = List.of(openProbe(1L), openProbe(2L));
        List<StaleUpdateLlmEntry> entries = List.of(
                new StaleUpdateLlmEntry(1L, "contradicted", "모순 근거"),
                new StaleUpdateLlmEntry(2L, "corrected", null)
        );

        List<StaleProbeUpdate> updates = AnthropicLiveTurnAnalyzerAdapter.toStaleUpdates(entries, openProbes);

        assertThat(updates).hasSize(2);
        assertThat(updates.get(0).reason()).isEqualTo(QuestionCandidateStaleReason.CONTRADICTED);
        assertThat(updates.get(1).reason()).isEqualTo(QuestionCandidateStaleReason.CORRECTED);
    }

    @Test
    void 스키마_밖_reason_값은_예외_없이_해당_항목만_제외한다() {
        List<QuestionCandidate> openProbes = List.of(openProbe(1L), openProbe(2L));
        List<StaleUpdateLlmEntry> entries = List.of(
                new StaleUpdateLlmEntry(1L, "unanswered", null),
                new StaleUpdateLlmEntry(2L, "corrected", null)
        );

        List<StaleProbeUpdate> updates = AnthropicLiveTurnAnalyzerAdapter.toStaleUpdates(entries, openProbes);

        assertThat(updates).hasSize(1);
        assertThat(updates.get(0).probeId()).isEqualTo(2L);
        assertThat(updates.get(0).reason()).isEqualTo(QuestionCandidateStaleReason.CORRECTED);
    }

    @Test
    void reason_값의_대소문자는_구분하지_않는다() {
        List<QuestionCandidate> openProbes = List.of(openProbe(1L));
        List<StaleUpdateLlmEntry> entries = List.of(new StaleUpdateLlmEntry(1L, "Contradicted", null));

        List<StaleProbeUpdate> updates = AnthropicLiveTurnAnalyzerAdapter.toStaleUpdates(entries, openProbes);

        assertThat(updates).hasSize(1);
        assertThat(updates.get(0).reason()).isEqualTo(QuestionCandidateStaleReason.CONTRADICTED);
    }

    @Test
    void open_probes에_없는_probeId는_기존과_동일하게_제외한다() {
        List<QuestionCandidate> openProbes = List.of(openProbe(1L));
        List<StaleUpdateLlmEntry> entries = List.of(new StaleUpdateLlmEntry(99L, "contradicted", null));

        List<StaleProbeUpdate> updates = AnthropicLiveTurnAnalyzerAdapter.toStaleUpdates(entries, openProbes);

        assertThat(updates).isEmpty();
    }
}

package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.adapter.out.integration.anthropic.ReportCardContentLlmEntry.HighlightSpanLlmEntry;
import com.yapp.d14.interview.domain.HighlightReason;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// LLM 응답 → HighlightSpan 매핑에서 reason 폴백과 followUpQuestions 게이팅 규칙을 고정한다(#78).
class ReportCardHighlightMappingTest {

    private static HighlightSpanLlmEntry entry(String tone, String reason, List<String> followUps) {
        return new HighlightSpanLlmEntry(0, 5, tone, reason, "제목", "분석", followUps);
    }

    @Test
    void PROBE_WORTHY이면_꼬리질문을_그대로_유지한다() {
        HighlightSpan span = AnthropicReportCardContentGeneratorAdapter.toHighlightSpan(
                entry("IMPROVE", "PROBE_WORTHY", List.of("왜 그렇게 했나요?", "대안은요?")));

        assertThat(span.reason()).isEqualTo(HighlightReason.PROBE_WORTHY);
        assertThat(span.followUpQuestions()).containsExactly("왜 그렇게 했나요?", "대안은요?");
    }

    @Test
    void PROBE_WORTHY가_아니면_꼬리질문이_있어도_비운다() {
        for (String reason : List.of("OFF_INTENT", "SHALLOW", "SUFFICIENT")) {
            HighlightSpan span = AnthropicReportCardContentGeneratorAdapter.toHighlightSpan(
                    entry("IMPROVE", reason, List.of("남아있으면 안 되는 질문")));
            assertThat(span.followUpQuestions()).as("reason=%s", reason).isEmpty();
        }
    }

    @Test
    void reason이_null이고_꼬리질문이_있으면_PROBE_WORTHY로_폴백한다() {
        HighlightSpan span = AnthropicReportCardContentGeneratorAdapter.toHighlightSpan(
                entry("GOOD", null, List.of("추가 질문")));

        assertThat(span.reason()).isEqualTo(HighlightReason.PROBE_WORTHY);
        assertThat(span.followUpQuestions()).containsExactly("추가 질문");
    }

    @Test
    void reason이_null이고_꼬리질문이_없으면_톤으로_폴백한다() {
        HighlightSpan good = AnthropicReportCardContentGeneratorAdapter.toHighlightSpan(
                entry("GOOD", null, List.of()));
        HighlightSpan improve = AnthropicReportCardContentGeneratorAdapter.toHighlightSpan(
                entry("IMPROVE", null, List.of()));

        assertThat(good.reason()).isEqualTo(HighlightReason.SUFFICIENT);
        assertThat(improve.reason()).isEqualTo(HighlightReason.SHALLOW);
    }

    @Test
    void 알_수_없는_reason_문자열은_폴백_규칙을_탄다() {
        HighlightSpan span = AnthropicReportCardContentGeneratorAdapter.toHighlightSpan(
                entry("IMPROVE", "GARBAGE", List.of()));

        assertThat(span.reason()).isEqualTo(HighlightReason.SHALLOW);
    }
}

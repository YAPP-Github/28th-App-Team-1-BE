package com.yapp.d14.interview.adapter.out.integration.anthropic;

import com.yapp.d14.interview.adapter.out.integration.anthropic.ReportCardContentLlmEntry.HighlightSpanLlmEntry;
import com.yapp.d14.interview.domain.HighlightReason;
import com.yapp.d14.interview.domain.HighlightSpan;
import com.yapp.d14.interview.domain.HighlightTone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// LLM 응답(quote 기반) → HighlightSpan 매핑에서 quote 탐색(순서 기반 커서)·reason 폴백·followUpQuestions
// 게이팅 규칙을 고정한다(#78).
class ReportCardHighlightMappingTest {

    private static HighlightSpanLlmEntry entry(String quote, String tone, String reason, List<String> followUps) {
        return new HighlightSpanLlmEntry(quote, tone, reason, "제목", "분석", followUps);
    }

    @Test
    void quote를_answerText에서_찾아_startIndex_endIndex를_계산한다() {
        String answerText = "저는 결제 서버 응답 속도를 개선했습니다.";

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(entry("결제 서버 응답 속도를 개선했습니다", "GOOD", "SUFFICIENT", List.of())), answerText);

        assertThat(spans).hasSize(1);
        HighlightSpan span = spans.get(0);
        int expectedStart = answerText.indexOf("결제 서버 응답 속도를 개선했습니다");
        assertThat(span.range().startIndex()).isEqualTo(expectedStart);
        assertThat(span.range().endIndex()).isEqualTo(expectedStart + "결제 서버 응답 속도를 개선했습니다".length());
    }

    @Test
    void 같은_문구가_반복되면_커서_이후_다음_등장을_순서대로_찾는다() {
        String answerText = "네 맞습니다. 그래서 다시 시도했습니다. 네 맞습니다. 결국 해결했습니다.";
        // "네 맞습니다"가 두 번 등장 — 두 하이라이트가 등장 순서대로 각각 다른 위치를 찾아야 한다.

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(
                        entry("네 맞습니다", "GOOD", "SUFFICIENT", List.of()),
                        entry("네 맞습니다", "GOOD", "SUFFICIENT", List.of())
                ), answerText);

        assertThat(spans).hasSize(2);
        int firstOccurrence = answerText.indexOf("네 맞습니다");
        int secondOccurrence = answerText.indexOf("네 맞습니다", firstOccurrence + 1);
        assertThat(spans.get(0).range().startIndex()).isEqualTo(firstOccurrence);
        assertThat(spans.get(1).range().startIndex()).isEqualTo(secondOccurrence);
        assertThat(spans.get(1).range().startIndex()).isGreaterThan(spans.get(0).range().endIndex());
    }

    @Test
    void quote가_answerText에_없으면_그_하이라이트만_제외한다() {
        String answerText = "실제로 있었던 답변 문장입니다.";

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(
                        entry("있었던 답변 문장", "GOOD", "SUFFICIENT", List.of()),
                        entry("답변에 없는 지어낸 문구", "GOOD", "SUFFICIENT", List.of())
                ), answerText);

        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).range().startIndex()).isEqualTo(answerText.indexOf("있었던 답변 문장"));
    }

    @Test
    void 순서_지시를_어겨도_전체_재검색으로_찾아낸다() {
        // 첫 하이라이트가 뒷부분(커서를 뒤로 이동)을 가리키고, 두 번째가 그보다 앞부분을 가리키는 경우.
        String answerText = "앞부분 문장입니다. 뒷부분 문장입니다.";

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(
                        entry("뒷부분 문장", "GOOD", "SUFFICIENT", List.of()),
                        entry("앞부분 문장", "GOOD", "SUFFICIENT", List.of())
                ), answerText);

        assertThat(spans).hasSize(2);
        assertThat(spans.get(0).range().startIndex()).isEqualTo(answerText.indexOf("뒷부분 문장"));
        assertThat(spans.get(1).range().startIndex()).isEqualTo(answerText.indexOf("앞부분 문장"));
    }

    @Test
    void quote가_null이거나_공백뿐이면_제외한다() {
        String answerText = "답변 문장입니다.";

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(entry(null, "GOOD", "SUFFICIENT", List.of()), entry("   ", "GOOD", "SUFFICIENT", List.of())),
                answerText);

        assertThat(spans).isEmpty();
    }

    @Test
    void PROBE_WORTHY이면_꼬리질문을_그대로_유지한다() {
        String answerText = "구체적인 경험 설명입니다.";

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(entry("구체적인 경험 설명", "IMPROVE", "PROBE_WORTHY", List.of("왜 그렇게 했나요?", "대안은요?"))),
                answerText);

        HighlightSpan span = spans.get(0);
        assertThat(span.reason()).isEqualTo(HighlightReason.PROBE_WORTHY);
        assertThat(span.followUpQuestions()).containsExactly("왜 그렇게 했나요?", "대안은요?");
    }

    @Test
    void PROBE_WORTHY가_아니면_꼬리질문이_있어도_비운다() {
        String answerText = "구체적인 경험 설명입니다.";
        for (String reason : List.of("OFF_INTENT", "SHALLOW", "SUFFICIENT")) {
            List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                    List.of(entry("구체적인 경험 설명", "IMPROVE", reason, List.of("남아있으면 안 되는 질문"))), answerText);
            assertThat(spans.get(0).followUpQuestions()).as("reason=%s", reason).isEmpty();
        }
    }

    @Test
    void reason이_null이고_꼬리질문이_있으면_PROBE_WORTHY로_폴백한다() {
        String answerText = "추가 설명이 필요한 답변입니다.";

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(entry("추가 설명이 필요한 답변", "GOOD", null, List.of("추가 질문"))), answerText);

        HighlightSpan span = spans.get(0);
        assertThat(span.reason()).isEqualTo(HighlightReason.PROBE_WORTHY);
        assertThat(span.followUpQuestions()).containsExactly("추가 질문");
    }

    @Test
    void reason이_null이고_꼬리질문이_없으면_톤으로_폴백한다() {
        String answerText = "좋은 답변입니다. 아쉬운 답변입니다.";

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(
                        entry("좋은 답변", "GOOD", null, List.of()),
                        entry("아쉬운 답변", "IMPROVE", null, List.of())
                ), answerText);

        assertThat(spans.get(0).reason()).isEqualTo(HighlightReason.SUFFICIENT);
        assertThat(spans.get(1).reason()).isEqualTo(HighlightReason.SHALLOW);
    }

    @Test
    void 알_수_없는_reason_문자열은_폴백_규칙을_탄다() {
        String answerText = "아쉬운 답변입니다.";

        List<HighlightSpan> spans = AnthropicReportCardContentGeneratorAdapter.toHighlightSpans(
                List.of(entry("아쉬운 답변", "IMPROVE", "GARBAGE", List.of())), answerText);

        assertThat(spans.get(0).reason()).isEqualTo(HighlightReason.SHALLOW);
    }
}

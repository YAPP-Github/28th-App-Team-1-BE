package com.yapp.d14.interview.adapter.out.integration.anthropic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicReportHeadlineGeneratorAdapterTest {

    @Test
    void 정상적인_한_문장은_그대로_반환한다() {
        String content = "캐시 도입 결정의 이유와 한계까지 구체적인 수치로 설명해주셨어요.";

        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize(content))
                .isEqualTo("캐시 도입 결정의 이유와 한계까지 구체적인 수치로 설명해주셨어요.");
    }

    @Test
    void 본문_아래에_붙은_경고_문단은_제거하고_첫_줄만_취한다() {
        String content = """
                이번 면접에서는 모듈 병합과 멱등성 문제 해결 경험을 중심으로 이야기를 나눴어요.

                ⚠️ 주의: 면접 중 질문 내용이 이전 발언에 없었던 정보를 가정하고 있었으며,
                실패 후 근본 원인 분석이 제시되지 않았습니다.""";

        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize(content))
                .isEqualTo("이번 면접에서는 모듈 병합과 멱등성 문제 해결 경험을 중심으로 이야기를 나눴어요.");
    }

    @Test
    void 앞뒤_공백과_빈_선행_줄을_건너뛰고_첫_실제_줄을_취한다() {
        String content = "\n\n   실제 첫 줄입니다.   \n다음 줄";

        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize(content))
                .isEqualTo("실제 첫 줄입니다.");
    }

    @Test
    void 감싸는_큰따옴표를_제거한다() {
        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize("\"따옴표로 감싼 문장이에요.\""))
                .isEqualTo("따옴표로 감싼 문장이에요.");
    }

    @Test
    void 감싸는_홑따옴표와_낫표도_제거한다() {
        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize("'홑따옴표 문장'"))
                .isEqualTo("홑따옴표 문장");
        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize("「낫표 문장」"))
                .isEqualTo("낫표 문장");
    }

    @Test
    void 문장_중간의_따옴표는_보존한다() {
        String content = "'플랫폼'이라는 표현을 사용해 설명해주셨어요.";

        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize(content))
                .isEqualTo("'플랫폼'이라는 표현을 사용해 설명해주셨어요.");
    }

    @Test
    void null이나_빈_문자열은_빈_문자열로_반환한다() {
        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize(null)).isEmpty();
        assertThat(AnthropicReportHeadlineGeneratorAdapter.sanitize("   \n  ")).isEmpty();
    }
}

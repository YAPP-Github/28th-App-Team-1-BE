package com.yapp.d14.portfolio.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioTest {

    private final Portfolio portfolio = Portfolio.create(
            UUID.randomUUID(), UUID.randomUUID(), "resume.pdf", 1024, 5, "users/x/portfolios/x/x.pdf"
    );

    @Test
    void 추출된_텍스트가_30자_이상이면_충분하다고_판단한다() {
        String text = "이 문장은 30자를 충분히 넘기는 추출된 텍스트입니다.";

        assertThat(portfolio.hasEnoughExtractedText(text)).isTrue();
    }

    @Test
    void 추출된_텍스트가_30자_미만이면_부족하다고_판단한다() {
        String text = "너무 짧은 텍스트";

        assertThat(portfolio.hasEnoughExtractedText(text)).isFalse();
    }

    @Test
    void 앞뒤_공백은_길이_판단에서_제외한다() {
        String text = "   짧음   ";

        assertThat(portfolio.hasEnoughExtractedText(text)).isFalse();
    }
}

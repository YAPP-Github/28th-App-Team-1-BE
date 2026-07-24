package com.yapp.d14.portfolio.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioTest {

    private final Portfolio portfolio = Portfolio.create(
            UUID.randomUUID(), UUID.randomUUID(), "resume.pdf", 1024, 5, "users/x/portfolios/x/x.pdf", false
    );

    @Test
    void 추출된_텍스트가_300자_이상이면_충분하다고_판단한다() {
        String text = "이 문장은 300자를 충분히 넘기는 추출된 텍스트인지 확인하기 위한 문장입니다. ".repeat(15);

        assertThat(portfolio.hasEnoughExtractedText(text)).isTrue();
    }

    @Test
    void 추출된_텍스트가_300자_미만이면_부족하다고_판단한다() {
        String text = "너무 짧은 텍스트";

        assertThat(portfolio.hasEnoughExtractedText(text)).isFalse();
    }

    @Test
    void 앞뒤_공백은_길이_판단에서_제외한다() {
        String text = "   짧음   ";

        assertThat(portfolio.hasEnoughExtractedText(text)).isFalse();
    }

    @Test
    void 재업로드로_생성되면_replacement가_true다() {
        Portfolio replacement = Portfolio.create(
                UUID.randomUUID(), UUID.randomUUID(), "resume.pdf", 1024, 5, "users/x/portfolios/x/x.pdf", true
        );

        assertThat(replacement.isReplacement()).isTrue();
    }

    @Test
    void softDelete하면_deleted_상태와_시각이_기록된다() {
        assertThat(portfolio.isDeleted()).isFalse();

        portfolio.softDelete();

        assertThat(portfolio.isDeleted()).isTrue();
        assertThat(portfolio.getDeletedAt()).isNotNull();
    }

    @Test
    void PROCESSING_상태로_생성된지_15초가_지나면_FAILED_SYSTEM으로_전환한다() {
        Portfolio staleProcessing = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(16));

        boolean timedOut = staleProcessing.failIfProcessingTimedOut();

        assertThat(timedOut).isTrue();
        assertThat(staleProcessing.getStatus()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
    }

    @Test
    void PROCESSING_상태로_생성된지_15초가_지나지_않았으면_상태를_유지한다() {
        Portfolio freshProcessing = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(5));

        boolean timedOut = freshProcessing.failIfProcessingTimedOut();

        assertThat(timedOut).isFalse();
        assertThat(freshProcessing.getStatus()).isEqualTo(PortfolioStatus.PROCESSING);
    }

    @Test
    void PROCESSING_상태가_아니면_시간이_지나도_상태를_유지한다() {
        Portfolio ready = Portfolio.of(
                UUID.randomUUID(), UUID.randomUUID(), "resume.pdf", 1024, 5, "users/x/portfolios/x/x.pdf",
                PortfolioStatus.READY, "완료", LocalDateTime.now().minusSeconds(100), LocalDateTime.now(),
                false, false, null
        );

        boolean timedOut = ready.failIfProcessingTimedOut();

        assertThat(timedOut).isFalse();
        assertThat(ready.getStatus()).isEqualTo(PortfolioStatus.READY);
    }

    private Portfolio processingPortfolioCreatedAt(LocalDateTime createdAt) {
        return Portfolio.of(
                UUID.randomUUID(), UUID.randomUUID(), "resume.pdf", 1024, 5, "users/x/portfolios/x/x.pdf",
                PortfolioStatus.PROCESSING, "포트폴리오를 분석하고 있어요.", createdAt, null,
                false, false, null
        );
    }
}

package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PortfolioProcessingTimeoutHandlerTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioEmbeddingStore portfolioEmbeddingStore;

    @Mock
    private PortfolioFileUploader portfolioFileUploader;

    @InjectMocks
    private PortfolioProcessingTimeoutHandler portfolioProcessingTimeoutHandler;

    @Test
    void 처리_시간을_넘겼으면_FAILED_SYSTEM으로_전환하고_저장한다() {
        Portfolio stale = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(16));

        boolean handled = portfolioProcessingTimeoutHandler.failAndCleanup(stale);

        assertThat(handled).isTrue();
        assertThat(stale.getStatus()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
        verify(portfolioRepository).save(stale);
    }

    @Test
    void 처리_시간을_넘겼으면_임베딩과_S3_원본을_함께_지운다() {
        Portfolio stale = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(16));

        portfolioProcessingTimeoutHandler.failAndCleanup(stale);

        verify(portfolioEmbeddingStore).deleteByPortfolioId(stale.getId());
        verify(portfolioFileUploader).delete(stale.getS3Key());
    }

    @Test
    void 소프트_삭제하지_않아_실패_사유가_계속_보인다() {
        Portfolio stale = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(16));

        portfolioProcessingTimeoutHandler.failAndCleanup(stale);

        assertThat(stale.isDeleted()).isFalse();
        assertThat(stale.getDeletedAt()).isNull();
    }

    @Test
    void 아직_처리_시간_안이면_아무것도_하지_않는다() {
        Portfolio fresh = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(5));

        boolean handled = portfolioProcessingTimeoutHandler.failAndCleanup(fresh);

        assertThat(handled).isFalse();
        assertThat(fresh.getStatus()).isEqualTo(PortfolioStatus.PROCESSING);
        verify(portfolioRepository, never()).save(any());
        verifyNoInteractions(portfolioEmbeddingStore, portfolioFileUploader);
    }

    @Test
    void PROCESSING이_아니면_아무것도_하지_않는다() {
        Portfolio ready = Portfolio.of(
                UUID.randomUUID(), UUID.randomUUID(), "resume.pdf", 1024, 5, "users/x/portfolios/x.pdf",
                PortfolioStatus.READY, "포트폴리오 처리가 완료되었습니다.", LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1), false, false, null
        );

        boolean handled = portfolioProcessingTimeoutHandler.failAndCleanup(ready);

        assertThat(handled).isFalse();
        verify(portfolioRepository, never()).save(any());
        verifyNoInteractions(portfolioEmbeddingStore, portfolioFileUploader);
    }

    private Portfolio processingPortfolioCreatedAt(LocalDateTime createdAt) {
        return Portfolio.of(
                UUID.randomUUID(), UUID.randomUUID(), "resume.pdf", 1024, 5, "users/x/portfolios/x.pdf",
                PortfolioStatus.PROCESSING, "포트폴리오를 분석하고 있어요.", createdAt, null,
                false, false, null
        );
    }
}

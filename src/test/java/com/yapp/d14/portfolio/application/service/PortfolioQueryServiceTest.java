package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioSummary;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioQueryServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @InjectMocks
    private PortfolioQueryService portfolioQueryService;

    private UUID userId;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolio = Portfolio.create(
                UUID.randomUUID(), userId, "resume.pdf", 1024, 5,
                "users/%s/portfolios/%s/test.pdf".formatted(userId, UUID.randomUUID()), false
        );
    }

    @Test
    void 이번달_재업로드_이력이_없으면_replaceAvailable이_true이고_nextAvailableAt은_null이다() {
        given(portfolioRepository.findAllActiveByUserId(userId)).willReturn(List.of(portfolio));
        given(portfolioRepository.existsReplacementSince(any(), any())).willReturn(false);

        List<PortfolioSummary> summaries = portfolioQueryService.getList(userId);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).replaceAvailable()).isTrue();
        assertThat(summaries.get(0).nextAvailableAt()).isNull();
    }

    @Test
    void 이번달_재업로드_이력이_있으면_replaceAvailable이_false이고_다음달_1일_0시를_반환한다() {
        given(portfolioRepository.findAllActiveByUserId(userId)).willReturn(List.of(portfolio));
        given(portfolioRepository.existsReplacementSince(any(), any())).willReturn(true);

        List<PortfolioSummary> summaries = portfolioQueryService.getList(userId);

        assertThat(summaries.get(0).replaceAvailable()).isFalse();
        assertThat(summaries.get(0).nextAvailableAt()).isEqualTo(PortfolioReplacementPolicy.nextMonthStart());
    }

    @Test
    void 상태_조회_시_PROCESSING이_15초를_넘었으면_FAILED_SYSTEM으로_전환하고_저장한다() {
        Portfolio stale = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(16));
        given(portfolioRepository.findById(stale.getId())).willReturn(Optional.of(stale));

        PortfolioStatusResult result = portfolioQueryService.getStatus(stale.getUserId(), stale.getId());

        assertThat(result.status()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
        verify(portfolioRepository).save(stale);
    }

    @Test
    void 상태_조회_시_PROCESSING이_15초를_넘지_않았으면_상태를_유지하고_저장하지_않는다() {
        Portfolio fresh = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(5));
        given(portfolioRepository.findById(fresh.getId())).willReturn(Optional.of(fresh));

        PortfolioStatusResult result = portfolioQueryService.getStatus(fresh.getUserId(), fresh.getId());

        assertThat(result.status()).isEqualTo(PortfolioStatus.PROCESSING);
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    void 목록_조회_시_PROCESSING이_15초를_넘은_항목을_FAILED_SYSTEM으로_전환하고_저장한다() {
        Portfolio stale = processingPortfolioCreatedAt(LocalDateTime.now().minusSeconds(16));
        given(portfolioRepository.findAllActiveByUserId(stale.getUserId())).willReturn(List.of(stale));
        given(portfolioRepository.existsReplacementSince(any(), any())).willReturn(false);

        List<PortfolioSummary> summaries = portfolioQueryService.getList(stale.getUserId());

        assertThat(summaries.get(0).status()).isEqualTo(PortfolioStatus.FAILED_SYSTEM);
        verify(portfolioRepository).save(stale);
    }

    private Portfolio processingPortfolioCreatedAt(LocalDateTime createdAt) {
        UUID ownerId = UUID.randomUUID();
        return Portfolio.of(
                UUID.randomUUID(), ownerId, "resume.pdf", 1024, 5, "users/x/portfolios/x/x.pdf",
                PortfolioStatus.PROCESSING, "포트폴리오를 분석하고 있어요.", createdAt, null,
                false, false, null
        );
    }
}

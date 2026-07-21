package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioSummary;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
}

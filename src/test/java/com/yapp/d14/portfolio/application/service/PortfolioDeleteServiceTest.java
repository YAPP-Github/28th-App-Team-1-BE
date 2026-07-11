package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioDeleteResult;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import com.yapp.d14.portfolio.exception.PortfolioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortfolioDeleteServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioFileUploader portfolioFileUploader;

    @Mock
    private PortfolioEmbeddingStore portfolioEmbeddingStore;

    @InjectMocks
    private PortfolioDeleteService portfolioDeleteService;

    private UUID userId;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        portfolio = Portfolio.create(
                UUID.randomUUID(), userId, "resume.pdf", 1024, 5,
                "users/%s/portfolios/%s/test.pdf".formatted(userId, UUID.randomUUID())
        );
    }

    @Test
    void 소유자가_아니면_예외를_던지고_삭제하지_않는다() {
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));
        UUID otherUserId = UUID.randomUUID();

        assertThatThrownBy(() -> portfolioDeleteService.delete(otherUserId, portfolio.getId()))
                .isInstanceOf(PortfolioException.class);

        verify(portfolioRepository, never()).deleteById(any());
    }

    @Test
    void 삭제하면_레코드와_벡터_S3_파일을_모두_정리한다() {
        given(portfolioRepository.findById(portfolio.getId())).willReturn(Optional.of(portfolio));

        PortfolioDeleteResult result = portfolioDeleteService.delete(userId, portfolio.getId());

        assertThat(result.portfolioId()).isEqualTo(portfolio.getId());
        verify(portfolioRepository).deleteById(portfolio.getId());
        verify(portfolioEmbeddingStore).deleteByPortfolioId(portfolio.getId());
        verify(portfolioFileUploader).delete(portfolio.getS3Key());
    }
}

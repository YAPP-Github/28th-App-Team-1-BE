package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.common.util.AfterCommitExecutor;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioDeleteResult;
import com.yapp.d14.portfolio.application.port.in.PortfolioDeleteUseCase;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import com.yapp.d14.portfolio.application.port.out.PortfolioFileUploader;
import com.yapp.d14.portfolio.application.port.out.PortfolioRepository;
import com.yapp.d14.portfolio.domain.Portfolio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioDeleteService implements PortfolioDeleteUseCase {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioFileUploader portfolioFileUploader;
    private final PortfolioEmbeddingStore portfolioEmbeddingStore;

    @Override
    @Transactional
    public PortfolioDeleteResult delete(UUID userId, UUID portfolioId) {
        Portfolio portfolio = PortfolioAccessSupport.requireOwned(portfolioRepository, portfolioId, userId);

        portfolioRepository.deleteById(portfolio.getId());
        AfterCommitExecutor.runAfterCommit(() -> {
            portfolioEmbeddingStore.deleteByPortfolioId(portfolio.getId());
            portfolioFileUploader.delete(portfolio.getS3Key());
        });

        return new PortfolioDeleteResult(portfolio.getId(), LocalDateTime.now());
    }
}

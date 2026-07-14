package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioSimilarityCheckUseCase;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioSimilarityCheckService implements PortfolioSimilarityCheckUseCase {

    private final PortfolioEmbeddingStore portfolioEmbeddingStore;

    @Override
    public Optional<Double> checkSimilarity(UUID portfolioId, String queryText) {
        return portfolioEmbeddingStore.findTopSimilarityScore(portfolioId, queryText);
    }
}

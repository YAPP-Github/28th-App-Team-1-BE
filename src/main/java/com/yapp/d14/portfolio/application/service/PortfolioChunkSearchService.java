package com.yapp.d14.portfolio.application.service;

import com.yapp.d14.portfolio.application.port.in.PortfolioChunkSearchUseCase;
import com.yapp.d14.portfolio.application.port.in.result.PortfolioChunkResult;
import com.yapp.d14.portfolio.application.port.out.PortfolioEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PortfolioChunkSearchService implements PortfolioChunkSearchUseCase {

    private final PortfolioEmbeddingStore portfolioEmbeddingStore;

    @Override
    public List<PortfolioChunkResult> searchChunks(UUID portfolioId, String queryText, int topK) {
        return portfolioEmbeddingStore.findTopChunks(portfolioId, queryText, topK).stream()
                .map(PortfolioChunkResult::new)
                .toList();
    }
}

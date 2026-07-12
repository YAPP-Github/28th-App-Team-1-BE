package com.yapp.d14.portfolio.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioEmbeddingStore {

    void save(UUID portfolioId, UUID userId, String fileName, String text);

    void deleteByPortfolioId(UUID portfolioId);

    Optional<Double> findTopSimilarityScore(UUID portfolioId, String queryText);

    List<String> findTopChunks(UUID portfolioId, String queryText, int topK);
}

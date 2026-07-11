package com.yapp.d14.portfolio.application.port.out;

import java.util.UUID;

public interface PortfolioEmbeddingStore {

    void save(UUID portfolioId, UUID userId, String fileName, String text);

    void deleteByPortfolioId(UUID portfolioId);
}

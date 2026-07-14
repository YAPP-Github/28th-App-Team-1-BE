package com.yapp.d14.portfolio.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface PortfolioSimilarityCheckUseCase {

    Optional<Double> checkSimilarity(UUID portfolioId, String queryText);
}

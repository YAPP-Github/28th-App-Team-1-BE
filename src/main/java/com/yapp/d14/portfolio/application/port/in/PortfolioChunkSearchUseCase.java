package com.yapp.d14.portfolio.application.port.in;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioChunkResult;

import java.util.List;
import java.util.UUID;

public interface PortfolioChunkSearchUseCase {

    List<PortfolioChunkResult> searchChunks(UUID portfolioId, String queryText, int topK);
}

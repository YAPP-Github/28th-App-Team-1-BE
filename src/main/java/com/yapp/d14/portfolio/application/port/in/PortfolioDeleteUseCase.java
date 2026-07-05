package com.yapp.d14.portfolio.application.port.in;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioDeleteResult;

import java.util.UUID;

public interface PortfolioDeleteUseCase {

    PortfolioDeleteResult delete(UUID userId, UUID portfolioId);
}

package com.yapp.d14.portfolio.application.port.in;

import java.util.UUID;

public interface PortfolioStatusUseCase {

    PortfolioStatusResult getStatus(UUID userId, UUID portfolioId);
}

package com.yapp.d14.portfolio.application.port.in;

import java.util.UUID;

public interface PortfolioProcessUseCase {

    void process(UUID userId, UUID portfolioId, byte[] fileContent);
}

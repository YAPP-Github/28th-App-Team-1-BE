package com.yapp.d14.portfolio.application.port.in;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioFileUrlResult;

import java.util.UUID;

public interface PortfolioFileUrlQueryUseCase {

    PortfolioFileUrlResult getFileUrl(UUID userId, UUID portfolioId);
}

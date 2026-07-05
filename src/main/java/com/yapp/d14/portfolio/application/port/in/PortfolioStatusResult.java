package com.yapp.d14.portfolio.application.port.in;

import com.yapp.d14.portfolio.domain.PortfolioStatus;

import java.util.UUID;

public record PortfolioStatusResult(
        UUID portfolioId,
        PortfolioStatus status,
        String message
) {
}

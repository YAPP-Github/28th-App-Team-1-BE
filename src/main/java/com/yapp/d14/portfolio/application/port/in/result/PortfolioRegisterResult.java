package com.yapp.d14.portfolio.application.port.in.result;

import com.yapp.d14.portfolio.domain.PortfolioStatus;

import java.util.UUID;

public record PortfolioRegisterResult(
        UUID portfolioId,
        PortfolioStatus status,
        String message
) {
}

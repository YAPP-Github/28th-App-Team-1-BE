package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.domain.PortfolioStatus;

import java.util.UUID;

public record PortfolioStatusHttpResponse(
        UUID portfolioId,
        PortfolioStatus status,
        String message
) {

    public static PortfolioStatusHttpResponse from(PortfolioStatusResult result) {
        return new PortfolioStatusHttpResponse(result.portfolioId(), result.status(), result.message());
    }
}

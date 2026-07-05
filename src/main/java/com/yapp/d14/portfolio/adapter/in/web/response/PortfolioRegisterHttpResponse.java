package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.PortfolioRegisterResult;
import com.yapp.d14.portfolio.domain.PortfolioStatus;

import java.util.UUID;

public record PortfolioRegisterHttpResponse(
        UUID portfolioId,
        PortfolioStatus status,
        String message
) {

    public static PortfolioRegisterHttpResponse from(PortfolioRegisterResult result) {
        return new PortfolioRegisterHttpResponse(result.portfolioId(), result.status(), result.message());
    }
}

package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.PortfolioDeleteResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioDeleteHttpResponse(
        UUID portfolioId,
        LocalDateTime deletedAt
) {

    public static PortfolioDeleteHttpResponse from(PortfolioDeleteResult result) {
        return new PortfolioDeleteHttpResponse(result.portfolioId(), result.deletedAt());
    }
}

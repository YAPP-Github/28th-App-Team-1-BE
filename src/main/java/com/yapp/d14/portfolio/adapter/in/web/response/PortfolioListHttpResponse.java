package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.PortfolioSummary;

import java.util.List;

public record PortfolioListHttpResponse(
        List<PortfolioSummaryHttpResponse> portfolios
) {

    public static PortfolioListHttpResponse from(List<PortfolioSummary> summaries) {
        return new PortfolioListHttpResponse(
                summaries.stream().map(PortfolioSummaryHttpResponse::from).toList()
        );
    }
}

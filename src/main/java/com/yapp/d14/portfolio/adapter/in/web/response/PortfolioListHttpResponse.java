package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PortfolioListHttpResponse(
        @Schema(description = "포트폴리오 목록 (MVP는 계정당 1건, 향후 다건 확장 고려해 배열로 응답)")
        List<PortfolioSummaryHttpResponse> portfolios
) {

    public static PortfolioListHttpResponse from(List<PortfolioSummary> summaries) {
        return new PortfolioListHttpResponse(
                summaries.stream().map(PortfolioSummaryHttpResponse::from).toList()
        );
    }
}

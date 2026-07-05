package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.PortfolioSummary;
import com.yapp.d14.portfolio.domain.PortfolioStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioSummaryHttpResponse(
        UUID portfolioId,
        String fileName,
        long fileSize,
        Integer pageCount,
        PortfolioStatus status,
        LocalDateTime uploadedAt
) {

    public static PortfolioSummaryHttpResponse from(PortfolioSummary summary) {
        return new PortfolioSummaryHttpResponse(
                summary.portfolioId(),
                summary.fileName(),
                summary.fileSize(),
                summary.pageCount(),
                summary.status(),
                summary.uploadedAt()
        );
    }
}

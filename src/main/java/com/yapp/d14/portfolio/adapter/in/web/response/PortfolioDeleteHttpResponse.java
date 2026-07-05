package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioDeleteResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioDeleteHttpResponse(
        @Schema(description = "삭제된 포트폴리오 ID")
        UUID portfolioId,

        @Schema(description = "삭제 처리 시각")
        LocalDateTime deletedAt
) {

    public static PortfolioDeleteHttpResponse from(PortfolioDeleteResult result) {
        return new PortfolioDeleteHttpResponse(result.portfolioId(), result.deletedAt());
    }
}

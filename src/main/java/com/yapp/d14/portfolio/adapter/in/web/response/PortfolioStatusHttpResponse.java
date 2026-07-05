package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioStatusResult;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record PortfolioStatusHttpResponse(
        @Schema(description = "조회한 포트폴리오 ID")
        UUID portfolioId,

        @Schema(description = "처리 상태 — PROCESSING(진행 중) / READY(완료) / FAILED_FILE·FAILED_SYSTEM(실패)")
        PortfolioStatus status,

        @Schema(description = "사용자에게 노출할 안내 메시지")
        String message
) {

    public static PortfolioStatusHttpResponse from(PortfolioStatusResult result) {
        return new PortfolioStatusHttpResponse(result.portfolioId(), result.status(), result.message());
    }
}

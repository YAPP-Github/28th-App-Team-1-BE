package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioRegisterResult;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record PortfolioRegisterHttpResponse(
        @Schema(description = "등록된 포트폴리오 ID")
        UUID portfolioId,

        @Schema(description = "등록 접수 직후 처리 상태, 항상 PROCESSING")
        PortfolioStatus status,

        @Schema(description = "사용자에게 노출할 안내 메시지")
        String message
) {

    public static PortfolioRegisterHttpResponse from(PortfolioRegisterResult result) {
        return new PortfolioRegisterHttpResponse(result.portfolioId(), result.status(), result.message());
    }
}

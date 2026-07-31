package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioFileUrlResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record PortfolioFileUrlHttpResponse(
        @Schema(description = "조회한 포트폴리오 ID")
        UUID portfolioId,

        @Schema(description = "PDF 열람용 presigned GET URL. 10분간만 유효합니다.")
        String fileUrl
) {

    public static PortfolioFileUrlHttpResponse from(PortfolioFileUrlResult result) {
        return new PortfolioFileUrlHttpResponse(result.portfolioId(), result.fileUrl());
    }
}

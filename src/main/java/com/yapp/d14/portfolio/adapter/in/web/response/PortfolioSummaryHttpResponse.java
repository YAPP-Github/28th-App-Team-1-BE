package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioSummary;
import com.yapp.d14.portfolio.domain.PortfolioStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioSummaryHttpResponse(
        @Schema(description = "포트폴리오 ID")
        UUID portfolioId,

        @Schema(description = "업로드 파일명", example = "portfolio.pdf")
        String fileName,

        @Schema(description = "파일 크기(byte)", example = "1048576")
        long fileSize,

        @Schema(description = "PDF 페이지 수", example = "12")
        Integer pageCount,

        @Schema(description = "처리 상태 — PROCESSING(진행 중) / READY(완료) / FAILED_FILE·FAILED_SYSTEM(실패)")
        PortfolioStatus status,

        @Schema(description = "업로드 시각")
        LocalDateTime uploadedAt,

        @Schema(description = "이번 달 재업로드(삭제 후 교체) 가능 여부")
        boolean replaceAvailable,

        @Schema(description = "재업로드가 막혀 있을 때 다시 가능해지는 시각(다음 달 1일 0시). 가능한 상태면 null")
        LocalDateTime nextAvailableAt
) {

    public static PortfolioSummaryHttpResponse from(PortfolioSummary summary) {
        return new PortfolioSummaryHttpResponse(
                summary.portfolioId(),
                summary.fileName(),
                summary.fileSize(),
                summary.pageCount(),
                summary.status(),
                summary.uploadedAt(),
                summary.replaceAvailable(),
                summary.nextAvailableAt()
        );
    }
}

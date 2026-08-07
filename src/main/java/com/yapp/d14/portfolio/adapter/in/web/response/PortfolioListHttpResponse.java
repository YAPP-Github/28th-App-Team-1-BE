package com.yapp.d14.portfolio.adapter.in.web.response;

import com.yapp.d14.portfolio.application.port.in.result.PortfolioListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record PortfolioListHttpResponse(
        @Schema(description = "이번 달 남은 재업로드(교체) 기회 여부. true면 1회 가능, false면 0회(업로드 시도 시 REPLACEMENT_LIMIT_EXCEEDED). READY까지 완료된 재업로드만 집계되며 취소·실패 건은 소진하지 않음. 삭제 기회와는 독립적으로 집계됨. 포트폴리오 존재 여부와 무관하게 계정 단위로 결정됨")
        boolean replaceAvailable,

        @Schema(description = "재업로드가 막혀 있을 때 다시 가능해지는 시각(다음 달 1일 0시). 가능한 상태면 null")
        LocalDateTime nextAvailableAt,

        @Schema(description = "이번 달 남은 삭제 기회 여부. true면 1회 가능, false면 0회(READY 상태 포트폴리오 삭제 시도 시 DELETE_LIMIT_EXCEEDED). 업로드 중 취소·처리 실패 건의 삭제는 false여도 항상 허용되며 기회를 소진하지 않음. 재업로드 기회와는 독립적으로 집계됨. 포트폴리오 존재 여부와 무관하게 계정 단위로 결정됨")
        boolean deleteAvailable,

        @Schema(description = "삭제가 막혀 있을 때 다시 가능해지는 시각(다음 달 1일 0시). 가능한 상태면 null")
        LocalDateTime nextDeleteAvailableAt,

        @Schema(description = "포트폴리오 목록 (MVP는 계정당 1건, 향후 다건 확장 고려해 배열로 응답)")
        List<PortfolioSummaryHttpResponse> portfolios
) {

    public static PortfolioListHttpResponse from(PortfolioListResult result) {
        return new PortfolioListHttpResponse(
                result.replaceAvailable(),
                result.nextAvailableAt(),
                result.deleteAvailable(),
                result.nextDeleteAvailableAt(),
                result.portfolios().stream().map(PortfolioSummaryHttpResponse::from).toList()
        );
    }
}

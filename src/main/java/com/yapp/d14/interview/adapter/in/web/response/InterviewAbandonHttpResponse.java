package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewAbandonResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record InterviewAbandonHttpResponse(
        @Schema(description = "면접 세션 ID")
        Long sessionId,

        @Schema(description = "세션 상태 — 항상 ABANDONED")
        String status,

        @Schema(description = "중단 사유 — NETWORK_DISCONNECT / USER_EXIT")
        String abandonCause,

        @Schema(description = "세션 종료 시각")
        LocalDateTime endedAt,

        @Schema(description = "이용권 처리 결과 — RELEASED(환급) / COMMITTED(차감예정)")
        String ticketOutcome,

        @Schema(description = "리포트 생성 트리거 여부(USER_EXIT이면 항상 true)")
        boolean reportGenerating
) {

    public static InterviewAbandonHttpResponse from(InterviewAbandonResult result) {
        return new InterviewAbandonHttpResponse(
                result.sessionId(),
                result.status(),
                result.abandonCause() == null ? null : result.abandonCause().name(),
                result.endedAt(),
                result.ticketOutcome(),
                result.reportGenerating()
        );
    }
}

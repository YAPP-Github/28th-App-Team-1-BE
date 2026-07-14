package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionStatusResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record InterviewSessionStatusHttpResponse(
        @Schema(description = "세션 준비 상태 — PROCESSING(진행 중) / READY(완료) / FAILED(실패)")
        String status,

        @Schema(description = "면접 시작 시각, READY일 때만 값 존재")
        LocalDateTime startedAt,

        @Schema(description = "요약 질문, READY일 때만 값 존재")
        SummaryQuestion summaryQuestion
) {

    public static InterviewSessionStatusHttpResponse from(InterviewSessionStatusResult result) {
        return new InterviewSessionStatusHttpResponse(
                result.status().name(),
                result.startedAt(),
                SummaryQuestion.from(result.summaryQuestion())
        );
    }

    public record SummaryQuestion(
            @Schema(description = "질문 ID")
            Long questionId,

            @Schema(description = "Base64 인코딩된 mp3 바이너리")
            String ttsAudio,

            @Schema(description = "턴 정보")
            Turn turn
    ) {

        private static SummaryQuestion from(InterviewSessionStatusResult.SummaryQuestion summaryQuestion) {
            if (summaryQuestion == null) {
                return null;
            }
            return new SummaryQuestion(
                    summaryQuestion.questionId(),
                    summaryQuestion.ttsAudio(),
                    new Turn(summaryQuestion.turnLevel(), summaryQuestion.depthLevel())
            );
        }
    }

    public record Turn(
            @Schema(description = "세션 전체 기준 순번")
            int turnLevel,

            @Schema(description = "현재 axis 내 순번")
            int depthLevel
    ) {
    }
}

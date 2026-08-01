package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewSessionResumeResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record InterviewSessionResumeHttpResponse(
        @Schema(description = "RESUMABLE(재개 가능) / ENDED(이미 종료됨)")
        String resumeState,

        @Schema(description = "면접 시작 시각. RESUMABLE일 때만 값 존재")
        LocalDateTime startedAt,

        @Schema(description = "면접 시작 후 경과 시간(초, 벽시계 기준). RESUMABLE일 때만 값 존재")
        Long elapsedSeconds,

        @Schema(description = "세션 종료 상태 — COMPLETED / ABANDONED / INVALID. ENDED일 때만 값 존재")
        String status
) {

    public static InterviewSessionResumeHttpResponse from(InterviewSessionResumeResult result) {
        return new InterviewSessionResumeHttpResponse(
                result.resumeState(), result.startedAt(), result.elapsedSeconds(), result.status()
        );
    }
}

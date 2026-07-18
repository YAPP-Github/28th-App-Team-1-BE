package com.yapp.d14.feedback.adapter.in.web.response;

import com.yapp.d14.feedback.application.port.in.result.FeedbackShareStatusResult;
import com.yapp.d14.feedback.domain.AttitudeAxis;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackShareStatusHttpResponse(
        @Schema(description = "공유 토큰. 클라이언트가 이 토큰으로 공유 딥링크를 조립한다.")
        String token,

        @Schema(description = "링크 상태 — ACTIVE(활성) / INVALIDATED(무효) / PRIVATE(비공개)", example = "ACTIVE")
        String status,

        @Schema(description = "지정된 태도 항목", example = "[\"GAZE\", \"EXPRESSION\"]")
        List<String> axes,

        @Schema(description = "제출한 지인 수(참고치, 최대 4)", example = "2")
        int submittedCount,

        @Schema(description = "영상 삭제 예정 시각")
        LocalDateTime videoExpiresAt,

        @Schema(description = "피드백 요청(최초 링크 생성) 시각")
        LocalDateTime requestedAt
) {

    public static FeedbackShareStatusHttpResponse from(FeedbackShareStatusResult result) {
        return new FeedbackShareStatusHttpResponse(
                result.token(),
                result.status().name(),
                result.axes().stream().map(AttitudeAxis::name).toList(),
                result.submittedCount(),
                result.videoExpiresAt(),
                result.requestedAt()
        );
    }
}

package com.yapp.d14.feedback.adapter.in.web.response;

import com.yapp.d14.feedback.application.port.in.result.GuestFeedbackEntryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GuestFeedbackEntryHttpResponse(
        @Schema(
                description = "게이트 판정 — OPEN(정상) / PRIVATE(비공개·무효) / EXPIRED(영상 만료) / FULL(정원 마감) / ALREADY_SUBMITTED(이 기기 제출 완료)",
                example = "OPEN"
        )
        String gate,

        @Schema(description = "피드백을 요청한 사용자 이름/별명", example = "재원")
        String requesterName,

        @Schema(description = "지인이 평가할 지정 항목")
        List<Axis> axes,

        @Schema(description = "면접 영상 파일 URL(S3 presigned). 만료 전까지 유효하며, 영상 파이프라인 연결 전까지 null.")
        String videoUrl,

        @Schema(description = "질문 경계(각 턴 시작 시각). 어떤 질문 구간인지 맥락 제공.")
        List<QuestionBoundary> questionBoundaries,

        @Schema(description = "제출 가능 여부. FULL/ALREADY_SUBMITTED/EXPIRED/PRIVATE면 false.", example = "true")
        boolean submissionOpen
) {

    public record Axis(
            @Schema(description = "항목 코드", example = "GAZE")
            String code,

            @Schema(description = "항목 표시명", example = "시선")
            String displayName
    ) {
    }

    public record QuestionBoundary(
            @Schema(description = "세션 전체 기준 순번", example = "1")
            int turnLevel,

            @Schema(description = "영상 내 질문 시작 시각(초)", example = "42.5")
            float startAt,

            @Schema(description = "질문 원문(AI 피드백 아님)")
            String questionText
    ) {
    }

    public static GuestFeedbackEntryHttpResponse from(GuestFeedbackEntryResult result) {
        List<Axis> axes = result.axes() == null ? List.of() : result.axes().stream()
                .map(axis -> new Axis(axis.name(), axis.getLabel()))
                .toList();
        List<QuestionBoundary> boundaries = result.questionBoundaries() == null ? List.of()
                : result.questionBoundaries().stream()
                .map(b -> new QuestionBoundary(b.turnLevel(), b.startAt(), b.questionText()))
                .toList();
        return new GuestFeedbackEntryHttpResponse(
                result.gate().name(),
                result.requesterName(),
                axes,
                result.videoUrl(),
                boundaries,
                result.gate().isSubmissionOpen()
        );
    }
}

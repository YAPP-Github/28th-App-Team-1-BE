package com.yapp.d14.interview.adapter.in.web.response;

import com.yapp.d14.interview.application.port.in.result.InterviewReportListItem;
import com.yapp.d14.interview.domain.JobType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewReportListHttpResponse(
        @Schema(description = "내 면접 레포트 목록 (최신순)")
        List<Item> reports
) {

    public static InterviewReportListHttpResponse from(List<InterviewReportListItem> items) {
        return new InterviewReportListHttpResponse(items.stream().map(Item::from).toList());
    }

    public record Item(
            @Schema(description = "면접 세션 ID", example = "1024")
            Long sessionId,

            @Schema(description = "직군 Enum 값 (스냅샷). 값이 없으면 null", example = "IOS", nullable = true)
            String jobType,

            @Schema(description = "직군 한글 표시명", example = "iOS", nullable = true)
            String jobTypeLabel,

            @Schema(description = "연차(년 단위, 스냅샷)", example = "2", nullable = true)
            Integer careerYears,

            @Schema(description = "면접 진행 시각", example = "2026-07-02T14:20:00")
            LocalDateTime interviewedAt,

            @Schema(description = "사용한 포트폴리오 파일명(스냅샷). 포트폴리오 없이 진행한 경우 null", example = "홍길동 자기소개서_SK프롬티어 기업 면접.pdf", nullable = true)
            String portfolioFileName,

            @Schema(description = "사용한 포트폴리오가 삭제되었는지 여부. true면 '삭제된 포트폴리오' 배지 표시", example = "false")
            boolean portfolioDeleted,

            @Schema(description = "JD URL. null이면 직접 입력했거나 JD 없이 진행한 것으로 '직접 입력함' 등으로 표기", example = "careers.skproptier.com/jobs/1024", nullable = true)
            String jdUrl,

            @Schema(description = "레포트 상태 — GENERATING/READY/INSUFFICIENT_ANALYSIS/FAILED", example = "READY")
            String reportStatus,

            @Schema(description = "지인 피드백 요청 가능 여부. 레포트가 READY이고 아직 공유 링크를 만들지 않았을 때 true", example = "true")
            boolean feedbackAvailable
    ) {

        public static Item from(InterviewReportListItem item) {
            JobType jobType = item.jobType();
            return new Item(
                    item.sessionId(),
                    jobType != null ? jobType.name() : null,
                    jobType != null ? jobType.getLabel() : null,
                    item.careerYears(),
                    item.interviewedAt(),
                    item.portfolioFileName(),
                    item.portfolioDeleted(),
                    item.jdUrl(),
                    item.reportStatus().name(),
                    item.feedbackAvailable()
            );
        }
    }
}

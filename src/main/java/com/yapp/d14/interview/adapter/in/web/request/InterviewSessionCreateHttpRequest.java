package com.yapp.d14.interview.adapter.in.web.request;

import com.yapp.d14.interview.application.command.InterviewSessionCreateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InterviewSessionCreateHttpRequest(
        @Schema(description = "면접에 활용할 포트폴리오 ID")
        @NotNull UUID portfolioId,

        @Schema(description = "직군", example = "BACKEND")
        @NotBlank String jobRole,

        @Schema(description = "연차(년 단위)", example = "1")
        @NotNull Integer careerYears,

        @Schema(description = "JD 링크(jdText와 상호 배타적)")
        String jdUrl,

        @Schema(description = "JD 원문 직접 입력(jdUrl과 상호 배타적)")
        String jdText,

        @Schema(description = "집중 프로젝트 설명, 10~300자")
        String freeText
) {

    public InterviewSessionCreateCommand toCommand(UUID userId) {
        return InterviewSessionCreateCommand.of(userId, portfolioId, jobRole, careerYears, jdUrl, jdText, freeText);
    }
}

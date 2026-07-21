package com.yapp.d14.user.adapter.in.web.response;

import com.yapp.d14.job.domain.Job;
import com.yapp.d14.user.application.port.in.result.UserProfileResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileHttpResponse(
        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "직무 Enum 값", example = "BACKEND")
        String jobRole,

        @Schema(description = "직무 한글 표시명", example = "백엔드")
        String jobRoleLabel,

        @Schema(description = "연차(년 단위)", example = "1")
        Integer careerYears,

        @Schema(description = "잔여 이용권 수", example = "3")
        int remainingTicketCount
) {

    public static UserProfileHttpResponse from(UserProfileResult result) {
        Job jobRole = result.jobRole();
        return new UserProfileHttpResponse(
                result.name(),
                jobRole != null ? jobRole.name() : null,
                jobRole != null ? jobRole.getLabel() : null,
                result.careerYears(),
                result.remainingTicketCount()
        );
    }
}

package com.yapp.d14.user.adapter.in.web.response;

import com.yapp.d14.user.application.port.in.result.UserProfileResult;
import com.yapp.d14.user.domain.JobRole;
import com.yapp.d14.user.domain.Provider;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserProfileHttpResponse(
        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "이메일. 소셜 계정에서 미제공 시 null", example = "jaewon@kakao.com", nullable = true)
        String email,

        @Schema(description = "소셜 로그인 제공자 — KAKAO / APPLE", example = "KAKAO")
        String provider,

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
        JobRole jobRole = result.jobRole();
        Provider provider = result.provider();
        return new UserProfileHttpResponse(
                result.name(),
                result.email(),
                provider != null ? provider.name() : null,
                jobRole != null ? jobRole.name() : null,
                jobRole != null ? jobRole.getLabel() : null,
                result.careerYears(),
                result.remainingTicketCount()
        );
    }
}

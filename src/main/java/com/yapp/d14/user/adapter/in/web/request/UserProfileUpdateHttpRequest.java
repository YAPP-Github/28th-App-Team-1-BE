package com.yapp.d14.user.adapter.in.web.request;

import com.yapp.d14.user.application.command.UserProfileUpdateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserProfileUpdateHttpRequest(
        @Schema(description = "이름(선택 — 변경할 때만 입력)", example = "홍길동")
        @Size(min = 1, max = 20, message = "이름은 1자 이상 20자 이하로 입력해주세요.")
        String name,

        @Schema(description = "직군", example = "BACKEND")
        @NotBlank(message = "직군을 입력해주세요.")
        String jobRole,

        @Schema(description = "연차(년 단위)", example = "1")
        @NotNull(message = "연차를 입력해주세요.")
        @Min(value = 0, message = "연차는 0년 이상이어야 해요.")
        @Max(value = 10, message = "연차는 10년 이하여야 해요.")
        Integer careerYears
) {

    public UserProfileUpdateCommand toCommand(UUID userId) {
        return UserProfileUpdateCommand.of(userId, name, jobRole, careerYears);
    }
}

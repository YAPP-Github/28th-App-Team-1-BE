package com.yapp.d14.jd.adapter.in.web.request;

import com.yapp.d14.jd.application.command.JdValidateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record JdValidateHttpRequest(
        @Schema(description = "크롤링할 JD URL", example = "https://example.com/careers/123")
        @NotBlank(message = "JD URL을 입력해주세요.")
        @Pattern(regexp = "^https?://.+", message = "올바른 URL 형식이 아니에요.")
        String jdUrl
) {

    public JdValidateCommand toCommand(UUID userId) {
        return new JdValidateCommand(userId, jdUrl);
    }
}

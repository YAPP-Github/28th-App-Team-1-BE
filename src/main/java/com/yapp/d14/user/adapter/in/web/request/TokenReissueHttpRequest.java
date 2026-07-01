package com.yapp.d14.user.adapter.in.web.request;

import com.yapp.d14.user.application.command.TokenReissueCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TokenReissueHttpRequest(
        @Schema(description = "리프레시 토큰")
        @NotBlank String refreshToken
) {

    public TokenReissueCommand toCommand() {
        return new TokenReissueCommand(refreshToken);
    }
}

package com.yapp.d14.auth.adapter.in.web.request;

import com.yapp.d14.auth.application.command.SocialLoginCommand;
import com.yapp.d14.user.domain.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginHttpRequest(
        @Schema(description = "소셜 로그인 제공자", example = "KAKAO")
        @NotNull Provider provider,

        @Schema(description = "카카오: 액세스 토큰 / 애플: authorization code")
        @NotBlank String credential
) {

    public SocialLoginCommand toCommand() {
        return new SocialLoginCommand(provider, credential);
    }
}

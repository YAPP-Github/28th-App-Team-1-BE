package com.yapp.d14.user.adapter.in.web.request;

import com.yapp.d14.user.application.command.UserSocialLoginCommand;
import com.yapp.d14.user.domain.Provider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserSocialLoginHttpRequest(
        @Schema(description = "소셜 로그인 제공자", example = "KAKAO")
        @NotNull Provider provider,

        @Schema(description = "카카오: 액세스 토큰 / 애플: authorization code")
        @NotBlank String credential
) {

    public UserSocialLoginCommand toCommand() {
        return new UserSocialLoginCommand(provider, credential);
    }
}

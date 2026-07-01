package com.yapp.d14.user.adapter.in.web.request;

import com.yapp.d14.user.application.command.UserSocialLoginCommand;
import com.yapp.d14.user.domain.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record UserSocialLoginHttpRequest(
        @NotNull Provider provider,
        @NotBlank String credential
) {

    UserSocialLoginCommand toCommand() {
        return new UserSocialLoginCommand(provider, credential);
    }
}

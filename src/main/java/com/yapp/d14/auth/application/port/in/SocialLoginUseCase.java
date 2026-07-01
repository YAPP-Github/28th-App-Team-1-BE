package com.yapp.d14.auth.application.port.in;

import com.yapp.d14.auth.application.command.SocialLoginCommand;

public interface SocialLoginUseCase {

    AuthToken login(SocialLoginCommand command);
}

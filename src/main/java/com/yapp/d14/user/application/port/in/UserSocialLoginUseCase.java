package com.yapp.d14.user.application.port.in;

import com.yapp.d14.user.application.command.UserSocialLoginCommand;

public interface UserSocialLoginUseCase {

    AuthToken login(UserSocialLoginCommand command);
}

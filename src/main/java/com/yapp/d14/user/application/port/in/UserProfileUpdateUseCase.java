package com.yapp.d14.user.application.port.in;

import com.yapp.d14.user.application.command.UserProfileUpdateCommand;

public interface UserProfileUpdateUseCase {

    void update(UserProfileUpdateCommand command);
}

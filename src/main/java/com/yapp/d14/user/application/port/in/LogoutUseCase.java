package com.yapp.d14.user.application.port.in;

import com.yapp.d14.user.application.command.LogoutCommand;

public interface LogoutUseCase {

    void logout(LogoutCommand command);
}

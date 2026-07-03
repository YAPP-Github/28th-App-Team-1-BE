package com.yapp.d14.auth.application.port.in;

import com.yapp.d14.auth.application.command.LogoutCommand;

public interface LogoutUseCase {

    void logout(LogoutCommand command);
}

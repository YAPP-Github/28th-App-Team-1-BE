package com.yapp.d14.auth.application.port.in;

import com.yapp.d14.auth.application.command.SocialUnlinkCommand;

public interface SocialUnlinkUseCase {

    void unlink(SocialUnlinkCommand command);
}

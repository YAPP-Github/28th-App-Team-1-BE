package com.yapp.d14.user.application.port.in;

import com.yapp.d14.user.application.command.TokenReissueCommand;

public interface TokenReissueUseCase {

    AuthToken reissue(TokenReissueCommand command);
}

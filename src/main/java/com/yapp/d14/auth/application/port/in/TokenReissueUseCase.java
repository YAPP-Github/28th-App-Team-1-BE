package com.yapp.d14.auth.application.port.in;

import com.yapp.d14.auth.application.command.TokenReissueCommand;
import com.yapp.d14.auth.application.port.in.result.AuthToken;

public interface TokenReissueUseCase {

    AuthToken reissue(TokenReissueCommand command);
}

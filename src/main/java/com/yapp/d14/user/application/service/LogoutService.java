package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.command.LogoutCommand;
import com.yapp.d14.user.application.port.in.LogoutUseCase;
import com.yapp.d14.user.application.port.out.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class LogoutService implements LogoutUseCase {

    private final TokenRepository tokenRepository;

    @Override
    public void logout(LogoutCommand command) {
        tokenRepository.delete(command.userId());
    }
}

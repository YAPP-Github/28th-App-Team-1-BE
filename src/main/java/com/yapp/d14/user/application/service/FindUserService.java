package com.yapp.d14.user.application.service;

import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.user.application.port.in.FindUserUseCase;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class FindUserService implements FindUserUseCase {

    private final UserRepository userRepository;

    @Override
    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
    }
}

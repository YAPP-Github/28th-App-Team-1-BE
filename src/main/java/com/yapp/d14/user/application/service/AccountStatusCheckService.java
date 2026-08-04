package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.port.in.AccountStatusCheckUseCase;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.User;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class AccountStatusCheckService implements AccountStatusCheckUseCase {

    private final UserRepository userRepository;

    @Override
    public void checkNotSuspended(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (user.isSuspended()) {
            throw new UserException(UserErrorCode.ACCOUNT_SUSPENDED);
        }
    }
}

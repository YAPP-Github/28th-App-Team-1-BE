package com.yapp.d14.user.application.service;

import com.yapp.d14.auth.application.command.LogoutCommand;
import com.yapp.d14.auth.application.port.in.LogoutUseCase;
import com.yapp.d14.common.util.AfterCommitExecutor;
import com.yapp.d14.user.application.port.in.UserWithdrawUseCase;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserWithdrawService implements UserWithdrawUseCase {

    private final UserRepository userRepository;
    private final LogoutUseCase logoutUseCase;

    @Override
    @Transactional
    public void withdraw(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        userRepository.deleteById(userId);

        // DB 삭제가 롤백되면 Redis 토큰은 무효화하지 않아야 하므로 커밋 이후로 미룬다.
        AfterCommitExecutor.runAfterCommit(() -> logoutUseCase.logout(new LogoutCommand(userId)));
    }
}

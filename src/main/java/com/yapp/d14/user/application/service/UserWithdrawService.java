package com.yapp.d14.user.application.service;

import com.yapp.d14.auth.application.command.LogoutCommand;
import com.yapp.d14.auth.application.command.SocialUnlinkCommand;
import com.yapp.d14.auth.application.port.in.LogoutUseCase;
import com.yapp.d14.auth.application.port.in.SocialUnlinkUseCase;
import com.yapp.d14.common.util.AfterCommitExecutor;
import com.yapp.d14.user.application.port.in.UserWithdrawUseCase;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.User;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class UserWithdrawService implements UserWithdrawUseCase {

    private final UserRepository userRepository;
    private final LogoutUseCase logoutUseCase;
    private final SocialUnlinkUseCase socialUnlinkUseCase;

    // 소셜 unlink/revoke는 외부 HTTP 호출이라 DB 트랜잭션으로 감싸지 않는다.
    // deleteById 자체는 Spring Data 리포지토리 구현체가 자체 트랜잭션으로 처리하므로
    // 여기서 별도로 @Transactional을 걸지 않아도 원자성이 깨지지 않는다.
    @Override
    public void withdraw(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        socialUnlinkUseCase.unlink(
                new SocialUnlinkCommand(user.getProvider(), user.getProviderId(), user.getAppleRefreshToken())
        );

        deleteAndLogout(userId);
    }

    private void deleteAndLogout(UUID userId) {
        try {
            userRepository.deleteById(userId);
        } catch (RuntimeException e) {
            // 소셜 연동은 이미 해제되어 되돌릴 수 없는 상태에서 계정 삭제만 실패한 것이므로
            // 운영자가 즉시 알아채고 수동 조치할 수 있도록 명확히 남긴다.
            log.error("[USER WITHDRAW] 소셜 연동 해제 후 계정 삭제 실패로 상태 불일치 발생: userId={}", userId, e);
            throw e;
        }

        // 위 deleteById가 이미 커밋된 뒤이므로 활성 트랜잭션이 없어 즉시 실행된다.
        AfterCommitExecutor.runAfterCommit(() -> logoutUseCase.logout(new LogoutCommand(userId)));
    }
}

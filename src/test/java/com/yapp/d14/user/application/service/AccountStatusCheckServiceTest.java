package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.AccountStatus;
import com.yapp.d14.user.domain.JobRole;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.SuspensionReason;
import com.yapp.d14.user.domain.User;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AccountStatusCheckServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountStatusCheckService accountStatusCheckService;

    private final UUID userId = UUID.randomUUID();

    private User user(AccountStatus accountStatus, SuspensionReason reason, LocalDateTime scheduledReleaseAt) {
        LocalDateTime now = LocalDateTime.now();
        return User.of(
                userId, "a@a.com", "이름", true, Provider.KAKAO, "pid", JobRole.BACKEND, 3, null,
                accountStatus, reason, accountStatus == AccountStatus.SUSPENDED ? now : null, scheduledReleaseAt,
                now, now
        );
    }

    @Test
    void 정상_계정이면_통과한다() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user(AccountStatus.ACTIVE, null, null)));

        assertThatCode(() -> accountStatusCheckService.checkNotSuspended(userId)).doesNotThrowAnyException();
    }

    @Test
    void 정지된_계정이면_ACCOUNT_SUSPENDED() {
        given(userRepository.findById(userId))
                .willReturn(Optional.of(user(AccountStatus.SUSPENDED, SuspensionReason.ABNORMAL_USAGE, null)));

        assertThatThrownBy(() -> accountStatusCheckService.checkNotSuspended(userId))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.ACCOUNT_SUSPENDED);
    }

    @Test
    void 해제_예정일이_지나도_자동으로_풀리지_않는다() {
        // 해제는 운영자 수동 처리만 지원한다(PRD Part7 계정 정지 정책).
        given(userRepository.findById(userId)).willReturn(Optional.of(
                user(AccountStatus.SUSPENDED, SuspensionReason.OPS_MANUAL, LocalDateTime.now().minusDays(1))
        ));

        assertThatThrownBy(() -> accountStatusCheckService.checkNotSuspended(userId))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.ACCOUNT_SUSPENDED);
    }

    @Test
    void 계정_상태가_비어_있는_기존_행은_정상으로_취급한다() {
        // ddl-auto: update로 컬럼이 추가된 직후의 기존 사용자 행을 흉내낸다.
        given(userRepository.findById(userId)).willReturn(Optional.of(user(null, null, null)));

        assertThatCode(() -> accountStatusCheckService.checkNotSuspended(userId)).doesNotThrowAnyException();
    }

    @Test
    void 존재하지_않는_사용자면_USER_NOT_FOUND() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountStatusCheckService.checkNotSuspended(userId))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}

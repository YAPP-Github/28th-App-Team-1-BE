package com.yapp.d14.user.application.service;

import com.yapp.d14.auth.application.command.LogoutCommand;
import com.yapp.d14.auth.application.command.SocialUnlinkCommand;
import com.yapp.d14.auth.application.port.in.LogoutUseCase;
import com.yapp.d14.auth.application.port.in.SocialUnlinkUseCase;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserWithdrawServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogoutUseCase logoutUseCase;

    @Mock
    private SocialUnlinkUseCase socialUnlinkUseCase;

    @InjectMocks
    private UserWithdrawService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 소셜_연동_해제에_성공하면_유저를_삭제하고_토큰을_무효화한다() {
        User user = User.create("a@a.com", Provider.KAKAO, "pid");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.withdraw(userId);

        verify(socialUnlinkUseCase).unlink(new SocialUnlinkCommand(Provider.KAKAO, "pid", null));
        verify(userRepository).deleteById(userId);
        verify(logoutUseCase).logout(new LogoutCommand(userId));
    }

    @Test
    void 존재하지_않는_유저면_USER_NOT_FOUND() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(userId))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).deleteById(userId);
        verify(logoutUseCase, never()).logout(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 소셜_연동_해제에_실패하면_유저를_삭제하지_않는다() {
        User user = User.create("a@a.com", Provider.APPLE, "pid");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willThrow(new AuthException(AuthErrorCode.SOCIAL_RECONNECT_REQUIRED))
                .given(socialUnlinkUseCase).unlink(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> service.withdraw(userId))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.SOCIAL_RECONNECT_REQUIRED);

        verify(userRepository, never()).deleteById(userId);
        verify(logoutUseCase, never()).logout(org.mockito.ArgumentMatchers.any());
    }
}

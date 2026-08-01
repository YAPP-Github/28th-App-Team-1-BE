package com.yapp.d14.user.application.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserNameRegisterServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserNameRegisterService service;

    private final UUID userId = UUID.randomUUID();

    private User newUser() {
        return User.create("a@a.com", Provider.KAKAO, "pid");
    }

    @Test
    void 정상_등록하면_이름이_설정된다() {
        User user = newUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.register(userId, "홍길동");

        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.isProfileRegistered()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void 다른_유저와_같은_이름이어도_등록된다() {
        User user = newUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.register(userId, "홍길동");

        assertThat(user.getName()).isEqualTo("홍길동");
        verify(userRepository).save(user);
    }

    @Test
    void 존재하지_않는_유저면_USER_NOT_FOUND() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(userId, "홍길동"))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}

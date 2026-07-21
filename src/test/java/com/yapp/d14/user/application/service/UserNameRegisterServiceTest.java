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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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
    void 정상_등록하면_이름이_설정되고_등록여부가_true가_된다() {
        User user = newUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNameAndIdNot("홍길동", userId)).willReturn(false);

        service.register(userId, "홍길동");

        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.isNameRegistered()).isTrue();
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

    @Test
    void 다른_유저가_이미_쓰는_이름이면_NAME_ALREADY_TAKEN() {
        given(userRepository.findById(userId)).willReturn(Optional.of(newUser()));
        given(userRepository.existsByNameAndIdNot("홍길동", userId)).willReturn(true);

        assertThatThrownBy(() -> service.register(userId, "홍길동"))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.NAME_ALREADY_TAKEN);
    }

    @Test
    void 본인이_이미_등록한_이름을_재제출해도_충돌이_아니다() {
        User user = newUser();
        user.registerName("홍길동");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNameAndIdNot("홍길동", userId)).willReturn(false);

        assertThatCode(() -> service.register(userId, "홍길동")).doesNotThrowAnyException();
    }

    @Test
    void 저장시점에_유니크제약이_위반되면_NAME_ALREADY_TAKEN으로_변환된다() {
        given(userRepository.findById(userId)).willReturn(Optional.of(newUser()));
        given(userRepository.existsByNameAndIdNot("홍길동", userId)).willReturn(false);
        willThrow(new DataIntegrityViolationException("duplicate")).given(userRepository).save(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> service.register(userId, "홍길동"))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.NAME_ALREADY_TAKEN);
    }
}

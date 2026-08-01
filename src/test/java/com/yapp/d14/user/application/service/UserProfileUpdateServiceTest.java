package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.command.UserProfileUpdateCommand;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.JobRole;
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
class UserProfileUpdateServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileUpdateService service;

    private final UUID userId = UUID.randomUUID();

    private User existingUser() {
        return User.create("a@a.com", Provider.KAKAO, "pid");
    }

    @Test
    void 이름을_포함해_직군과_연차를_수정한다() {
        User user = existingUser();
        user.registerName("기존이름");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.update(new UserProfileUpdateCommand(userId, "기존이름", JobRole.BACKEND, 3));

        assertThat(user.getName()).isEqualTo("기존이름");
        assertThat(user.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(user.getCareerYears()).isEqualTo(3);
        verify(userRepository).save(user);
    }

    @Test
    void 이름을_변경하면_반영된다() {
        User user = existingUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.update(new UserProfileUpdateCommand(userId, "새이름", JobRole.BACKEND, 3));

        assertThat(user.getName()).isEqualTo("새이름");
        assertThat(user.isProfileRegistered()).isTrue();
    }

    @Test
    void 다른_유저와_같은_이름으로_변경해도_반영된다() {
        User user = existingUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.update(new UserProfileUpdateCommand(userId, "새이름", JobRole.BACKEND, 3));

        assertThat(user.getName()).isEqualTo("새이름");
        verify(userRepository).save(user);
    }

    @Test
    void 기존값이_있어도_무조건_덮어쓴다() {
        User user = existingUser();
        user.updateProfile(JobRole.FRONTEND, 5);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.update(new UserProfileUpdateCommand(userId, "새이름", JobRole.BACKEND, 1));

        assertThat(user.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(user.getCareerYears()).isEqualTo(1);
    }

    @Test
    void 존재하지_않는_유저면_USER_NOT_FOUND() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new UserProfileUpdateCommand(userId, "새이름", JobRole.BACKEND, 1)))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}

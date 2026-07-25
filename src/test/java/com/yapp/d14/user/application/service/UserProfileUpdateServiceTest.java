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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
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
    void 이름없이_직군과_연차만_수정하면_이름은_그대로다() {
        User user = existingUser();
        user.registerName("기존이름");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.update(new UserProfileUpdateCommand(userId, null, JobRole.BACKEND, 3));

        assertThat(user.getName()).isEqualTo("기존이름");
        assertThat(user.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(user.getCareerYears()).isEqualTo(3);
        verify(userRepository, never()).existsByNameAndIdNot(any(), any());
        verify(userRepository).save(user);
    }

    @Test
    void 이름도_함께_보내면_중복확인_후_변경된다() {
        User user = existingUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByNameAndIdNot("새이름", userId)).willReturn(false);

        service.update(new UserProfileUpdateCommand(userId, "새이름", JobRole.BACKEND, 3));

        assertThat(user.getName()).isEqualTo("새이름");
        assertThat(user.isNameRegistered()).isTrue();
    }

    @Test
    void 다른_유저가_쓰는_이름으로_변경하면_NAME_ALREADY_TAKEN() {
        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser()));
        given(userRepository.existsByNameAndIdNot("새이름", userId)).willReturn(true);

        assertThatThrownBy(() -> service.update(new UserProfileUpdateCommand(userId, "새이름", JobRole.BACKEND, 3)))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.NAME_ALREADY_TAKEN);
    }

    @Test
    void 저장시점_유니크제약_위반도_NAME_ALREADY_TAKEN으로_변환된다() {
        given(userRepository.findById(userId)).willReturn(Optional.of(existingUser()));
        given(userRepository.existsByNameAndIdNot("새이름", userId)).willReturn(false);
        willThrow(new DataIntegrityViolationException("duplicate")).given(userRepository).save(any());

        assertThatThrownBy(() -> service.update(new UserProfileUpdateCommand(userId, "새이름", JobRole.BACKEND, 3)))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.NAME_ALREADY_TAKEN);
    }

    @Test
    void 기존값이_있어도_무조건_덮어쓴다() {
        User user = existingUser();
        user.updateProfile(JobRole.FRONTEND, 5);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.update(new UserProfileUpdateCommand(userId, null, JobRole.BACKEND, 1));

        assertThat(user.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(user.getCareerYears()).isEqualTo(1);
    }

    @Test
    void 존재하지_않는_유저면_USER_NOT_FOUND() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new UserProfileUpdateCommand(userId, null, JobRole.BACKEND, 1)))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}

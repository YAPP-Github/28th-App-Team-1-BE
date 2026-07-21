package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.JobRole;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserProfileInitializeServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileInitializeService service;

    private final UUID userId = UUID.randomUUID();

    private User emptyProfileUser() {
        return User.create("a@a.com", Provider.KAKAO, "pid");
    }

    @Test
    void 둘다_비어있으면_둘다_셋팅한다() {
        User user = emptyProfileUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.initializeIfAbsent(userId, "BACKEND", 3);

        assertThat(user.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(user.getCareerYears()).isEqualTo(3);
        verify(userRepository).save(user);
    }

    @Test
    void jobRole만_비어있으면_jobRole만_채우고_기존_careerYears는_유지한다() {
        User user = emptyProfileUser();
        user.updateProfile(null, 7);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.initializeIfAbsent(userId, "FRONTEND", 3);

        assertThat(user.getJobRole()).isEqualTo(JobRole.FRONTEND);
        assertThat(user.getCareerYears()).isEqualTo(7);
    }

    @Test
    void 둘다_있으면_아무것도_하지_않는다() {
        User user = emptyProfileUser();
        user.updateProfile(JobRole.BACKEND, 5);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.initializeIfAbsent(userId, "FRONTEND", 1);

        assertThat(user.getJobRole()).isEqualTo(JobRole.BACKEND);
        assertThat(user.getCareerYears()).isEqualTo(5);
        verify(userRepository, never()).save(user);
    }

    @Test
    void 유저가_없으면_예외없이_리턴한다() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatCode(() -> service.initializeIfAbsent(userId, "BACKEND", 3)).doesNotThrowAnyException();
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 잘못된_rawJobRole이면_jobRole은_null로_남고_careerYears는_셋팅된다() {
        User user = emptyProfileUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertThatCode(() -> service.initializeIfAbsent(userId, "NOT_A_JOB", 4)).doesNotThrowAnyException();

        assertThat(user.getJobRole()).isNull();
        assertThat(user.getCareerYears()).isEqualTo(4);
    }
}

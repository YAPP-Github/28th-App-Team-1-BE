package com.yapp.d14.user.application.service;

import com.yapp.d14.job.domain.Job;
import com.yapp.d14.ticket.application.port.in.TicketRemainingQueryUseCase;
import com.yapp.d14.user.application.port.in.result.UserProfileResult;
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

@ExtendWith(MockitoExtension.class)
class UserProfileQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketRemainingQueryUseCase ticketRemainingQueryUseCase;

    @InjectMocks
    private UserProfileQueryService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void 유저정보와_잔여이용권을_조합해_반환한다() {
        User user = User.create("a@a.com", Provider.KAKAO, "pid");
        user.registerName("홍길동");
        user.updateProfile(Job.BACKEND, 3);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ticketRemainingQueryUseCase.getRemaining(userId)).willReturn(2);

        UserProfileResult result = service.getProfile(userId);

        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.nameRegistered()).isTrue();
        assertThat(result.jobRole()).isEqualTo(Job.BACKEND);
        assertThat(result.careerYears()).isEqualTo(3);
        assertThat(result.remainingTicketCount()).isEqualTo(2);
    }

    @Test
    void 프로필이_비어있으면_jobRole과_careerYears가_null이다() {
        User user = User.create("a@a.com", Provider.KAKAO, "pid");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(ticketRemainingQueryUseCase.getRemaining(userId)).willReturn(3);

        UserProfileResult result = service.getProfile(userId);

        assertThat(result.jobRole()).isNull();
        assertThat(result.careerYears()).isNull();
        assertThat(result.nameRegistered()).isFalse();
    }

    @Test
    void 존재하지_않는_유저면_USER_NOT_FOUND() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(userId))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getErrorCode())
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}

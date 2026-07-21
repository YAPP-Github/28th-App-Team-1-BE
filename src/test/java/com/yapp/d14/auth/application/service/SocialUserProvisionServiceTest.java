package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialUserProvisionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SocialUserProvisionService service;

    @Test
    void 기존_유저면_그대로_반환하고_저장하지_않는다() {
        User existing = User.create("a@a.com", Provider.KAKAO, "pid");
        given(userRepository.findByProviderAndProviderId(Provider.KAKAO, "pid")).willReturn(Optional.of(existing));

        User result = service.provision(Provider.KAKAO, new SocialUserInfo("pid", "a@a.com", "카카오닉네임"));

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 신규_유저면_소셜_닉네임과_무관하게_이름없이_생성된다() {
        given(userRepository.findByProviderAndProviderId(Provider.KAKAO, "pid")).willReturn(Optional.empty());
        given(userRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        service.provision(Provider.KAKAO, new SocialUserInfo("pid", "a@a.com", "카카오닉네임"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isNull();
        assertThat(captor.getValue().isNameRegistered()).isFalse();
    }
}

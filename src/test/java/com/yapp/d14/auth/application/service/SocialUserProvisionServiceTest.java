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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocialUserProvisionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SocialUserProvisionService service;

    @Test
    void 기존_유저면_그대로_반환하고_저장하지_않으며_신규_여부는_false다() {
        User existing = User.create("a@a.com", Provider.KAKAO, "pid");
        given(userRepository.findByProviderAndProviderId(Provider.KAKAO, "pid")).willReturn(Optional.of(existing));

        UserProvisionResult result = service.provision(Provider.KAKAO, new SocialUserInfo("pid", "a@a.com", "카카오닉네임", null));

        assertThat(result.user()).isEqualTo(existing);
        assertThat(result.newlyCreated()).isFalse();
        verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 신규_유저면_소셜_닉네임과_무관하게_이름없이_생성되고_신규_여부는_true다() {
        given(userRepository.findByProviderAndProviderId(Provider.KAKAO, "pid")).willReturn(Optional.empty());
        given(userRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        UserProvisionResult result = service.provision(Provider.KAKAO, new SocialUserInfo("pid", "a@a.com", "카카오닉네임", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isNull();
        assertThat(captor.getValue().isProfileRegistered()).isFalse();
        assertThat(result.newlyCreated()).isTrue();
    }

    @Test
    void 애플_신규_유저는_refresh_token을_저장한_뒤_반환하고_신규_여부는_true다() {
        given(userRepository.findByProviderAndProviderId(Provider.APPLE, "pid")).willReturn(Optional.empty());
        given(userRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        UserProvisionResult result = service.provision(Provider.APPLE, new SocialUserInfo("pid", "a@a.com", null, "apple-refresh-token"));

        assertThat(result.user().getAppleRefreshToken()).isEqualTo("apple-refresh-token");
        assertThat(result.newlyCreated()).isTrue();
        verify(userRepository, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 애플_기존_유저도_재로그인하면_refresh_token이_갱신되고_신규_여부는_false다() {
        User existing = User.create("a@a.com", Provider.APPLE, "pid");
        given(userRepository.findByProviderAndProviderId(Provider.APPLE, "pid")).willReturn(Optional.of(existing));
        given(userRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        UserProvisionResult result = service.provision(Provider.APPLE, new SocialUserInfo("pid", "a@a.com", null, "new-refresh-token"));

        assertThat(result.user().getAppleRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.newlyCreated()).isFalse();
        verify(userRepository).save(existing);
    }
}

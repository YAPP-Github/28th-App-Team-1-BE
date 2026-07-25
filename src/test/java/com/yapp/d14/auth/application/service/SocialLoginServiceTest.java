package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.command.SocialLoginCommand;
import com.yapp.d14.auth.application.port.in.result.AuthToken;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.application.port.out.SocialAuthClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.auth.application.port.out.TokenRepository;
import com.yapp.d14.user.application.port.in.UserProfileQueryUseCase;
import com.yapp.d14.user.application.port.in.result.UserProfileResult;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock
    private SocialAuthClient socialAuthClient;

    @Mock
    private SocialUserProvisionService socialUserProvisionService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserProfileQueryUseCase userProfileQueryUseCase;

    @InjectMocks
    private SocialLoginService service;

    @Test
    void 로그인에_성공하면_토큰과_프로필_스냅샷을_함께_반환한다() {
        User user = User.create("a@a.com", Provider.KAKAO, "pid");
        SocialUserInfo userInfo = new SocialUserInfo("pid", "a@a.com", "카카오닉네임");
        UserProfileResult profile = new UserProfileResult(user.getId(), "a@a.com", null, false, null, null, 3);

        given(socialAuthClient.getUserInfo(Provider.KAKAO, "credential")).willReturn(userInfo);
        given(socialUserProvisionService.provision(Provider.KAKAO, userInfo)).willReturn(user);
        given(jwtProvider.issueAccessToken(user.getId(), Provider.KAKAO)).willReturn("access");
        given(jwtProvider.issueRefreshToken(user.getId(), Provider.KAKAO)).willReturn("refresh");
        given(userProfileQueryUseCase.getProfile(user.getId())).willReturn(profile);

        AuthToken token = service.login(new SocialLoginCommand(Provider.KAKAO, "credential"));

        assertThat(token.accessToken()).isEqualTo("access");
        assertThat(token.refreshToken()).isEqualTo("refresh");
        assertThat(token.profile()).isEqualTo(profile);
    }
}

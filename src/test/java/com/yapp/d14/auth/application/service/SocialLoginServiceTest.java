package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.command.SocialLoginCommand;
import com.yapp.d14.auth.application.port.in.result.AuthToken;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.application.port.out.SocialAuthClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.auth.application.port.out.TokenRepository;
import com.yapp.d14.consent.application.port.in.RequiredConsentStatusQueryUseCase;
import com.yapp.d14.consent.domain.RequiredConsentStatus;
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

    @Mock
    private RequiredConsentStatusQueryUseCase requiredConsentStatusQueryUseCase;

    @InjectMocks
    private SocialLoginService service;

    @Test
    void 로그인에_성공하면_토큰과_프로필_스냅샷과_신규_회원_동의상태를_함께_반환한다() {
        User user = User.create("a@a.com", Provider.KAKAO, "pid");
        SocialUserInfo userInfo = new SocialUserInfo("pid", "a@a.com", "카카오닉네임", null);
        UserProfileResult profile = new UserProfileResult(user.getId(), "a@a.com", Provider.KAKAO, null, false, null, null, 3);
        UserProvisionResult provisionResult = new UserProvisionResult(user, true);

        given(socialAuthClient.getUserInfo(Provider.KAKAO, "credential")).willReturn(userInfo);
        given(socialUserProvisionService.provision(Provider.KAKAO, userInfo)).willReturn(provisionResult);
        given(jwtProvider.issueAccessToken(user.getId(), Provider.KAKAO)).willReturn("access");
        given(jwtProvider.issueRefreshToken(user.getId(), Provider.KAKAO)).willReturn("refresh");
        given(userProfileQueryUseCase.getProfile(user.getId())).willReturn(profile);
        given(requiredConsentStatusQueryUseCase.getStatus(user.getId())).willReturn(RequiredConsentStatus.NOT_SUBMITTED);

        AuthToken token = service.login(new SocialLoginCommand(Provider.KAKAO, "credential"));

        assertThat(token.accessToken()).isEqualTo("access");
        assertThat(token.refreshToken()).isEqualTo("refresh");
        assertThat(token.newUser()).isTrue();
        assertThat(token.consentStatus()).isEqualTo(RequiredConsentStatus.NOT_SUBMITTED);
        assertThat(token.profile()).isEqualTo(profile);
    }
}

package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.command.TokenReissueCommand;
import com.yapp.d14.auth.application.port.in.result.AuthToken;
import com.yapp.d14.auth.application.port.out.JwtClaims;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.application.port.out.TokenRepository;
import com.yapp.d14.consent.application.port.in.RequiredConsentStatusQueryUseCase;
import com.yapp.d14.consent.domain.RequiredConsentStatus;
import com.yapp.d14.user.application.port.in.UserProfileQueryUseCase;
import com.yapp.d14.user.application.port.in.result.UserProfileResult;
import com.yapp.d14.user.domain.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TokenReissueServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserProfileQueryUseCase userProfileQueryUseCase;

    @Mock
    private RequiredConsentStatusQueryUseCase requiredConsentStatusQueryUseCase;

    @InjectMocks
    private TokenReissueService service;

    @Test
    void 재발급에_성공하면_새_토큰과_프로필_스냅샷과_동의상태를_함께_반환하고_신규_회원_여부는_항상_false다() {
        UUID userId = UUID.randomUUID();
        JwtClaims claims = new JwtClaims(userId, Provider.KAKAO);
        UserProfileResult profile = new UserProfileResult(userId, "a@a.com", Provider.KAKAO, "홍길동", true, null, null, 3);

        given(jwtProvider.parseRefreshToken("oldRefresh")).willReturn(claims);
        given(tokenRepository.find(userId)).willReturn(Optional.of("oldRefresh"));
        given(jwtProvider.issueAccessToken(userId, Provider.KAKAO)).willReturn("newAccess");
        given(jwtProvider.issueRefreshToken(userId, Provider.KAKAO)).willReturn("newRefresh");
        given(userProfileQueryUseCase.getProfile(userId)).willReturn(profile);
        given(requiredConsentStatusQueryUseCase.getStatus(userId)).willReturn(RequiredConsentStatus.UP_TO_DATE);

        AuthToken token = service.reissue(new TokenReissueCommand("oldRefresh"));

        assertThat(token.accessToken()).isEqualTo("newAccess");
        assertThat(token.refreshToken()).isEqualTo("newRefresh");
        assertThat(token.newUser()).isFalse();
        assertThat(token.consentStatus()).isEqualTo(RequiredConsentStatus.UP_TO_DATE);
        assertThat(token.profile()).isEqualTo(profile);
    }
}

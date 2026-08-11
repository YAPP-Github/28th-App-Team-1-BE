package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.command.SocialLoginCommand;
import com.yapp.d14.auth.application.port.in.result.AuthToken;
import com.yapp.d14.auth.application.port.in.SocialLoginUseCase;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.application.port.out.SocialAuthClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.auth.application.port.out.TokenRepository;
import com.yapp.d14.common.properties.DemoLoginProperties;
import com.yapp.d14.consent.application.port.in.RequiredConsentStatusQueryUseCase;
import com.yapp.d14.consent.domain.RequiredConsentStatus;
import com.yapp.d14.user.application.port.in.FindUserUseCase;
import com.yapp.d14.user.application.port.in.UserProfileQueryUseCase;
import com.yapp.d14.user.application.port.in.result.UserProfileResult;
import com.yapp.d14.user.domain.Provider;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class SocialLoginService implements SocialLoginUseCase {

    private final SocialAuthClient socialAuthClient;
    private final SocialUserProvisionService socialUserProvisionService;
    private final TokenRepository tokenRepository;
    private final JwtProvider jwtProvider;
    private final UserProfileQueryUseCase userProfileQueryUseCase;
    private final RequiredConsentStatusQueryUseCase requiredConsentStatusQueryUseCase;
    private final FindUserUseCase findUserUseCase;
    private final DemoLoginProperties demoLoginProperties;

    @Override
    public AuthToken login(SocialLoginCommand command) {
        if (isDemoLogin(command)) {
            User demoUser = findUserUseCase.findById(UUID.fromString(demoLoginProperties.getUserId()));
            return issueAuthToken(demoUser, false);
        }

        SocialUserInfo userInfo = socialAuthClient.getUserInfo(command.provider(), command.credential());

        UserProvisionResult provisionResult = socialUserProvisionService.provision(command.provider(), userInfo);

        return issueAuthToken(provisionResult.user(), provisionResult.newlyCreated());
    }

    private boolean isDemoLogin(SocialLoginCommand command) {
        return demoLoginProperties.isEnabled()
                && command.provider() == Provider.KAKAO
                && command.credential().equals(demoLoginProperties.getUserId());
    }

    private AuthToken issueAuthToken(User user, boolean newUser) {
        UserProfileResult profile = userProfileQueryUseCase.getProfile(user.getId());
        RequiredConsentStatus consentStatus = requiredConsentStatusQueryUseCase.getStatus(user.getId());

        String accessToken = jwtProvider.issueAccessToken(user.getId(), user.getProvider());
        String refreshToken = jwtProvider.issueRefreshToken(user.getId(), user.getProvider());
        tokenRepository.save(user.getId(), refreshToken);

        return new AuthToken(accessToken, refreshToken, newUser, consentStatus, profile);
    }
}

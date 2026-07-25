package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.command.SocialLoginCommand;
import com.yapp.d14.auth.application.port.in.result.AuthToken;
import com.yapp.d14.auth.application.port.in.SocialLoginUseCase;
import com.yapp.d14.auth.application.port.out.JwtProvider;
import com.yapp.d14.auth.application.port.out.SocialAuthClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.auth.application.port.out.TokenRepository;
import com.yapp.d14.user.application.port.in.UserProfileQueryUseCase;
import com.yapp.d14.user.application.port.in.result.UserProfileResult;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class SocialLoginService implements SocialLoginUseCase {

    private final SocialAuthClient socialAuthClient;
    private final SocialUserProvisionService socialUserProvisionService;
    private final TokenRepository tokenRepository;
    private final JwtProvider jwtProvider;
    private final UserProfileQueryUseCase userProfileQueryUseCase;

    @Override
    public AuthToken login(SocialLoginCommand command) {
        SocialUserInfo userInfo = socialAuthClient.getUserInfo(command.provider(), command.credential());

        User user = socialUserProvisionService.provision(command.provider(), userInfo);

        String accessToken = jwtProvider.issueAccessToken(user.getId(), user.getProvider());
        String refreshToken = jwtProvider.issueRefreshToken(user.getId(), user.getProvider());
        tokenRepository.save(user.getId(), refreshToken);

        UserProfileResult profile = userProfileQueryUseCase.getProfile(user.getId());

        return new AuthToken(accessToken, refreshToken, profile);
    }
}

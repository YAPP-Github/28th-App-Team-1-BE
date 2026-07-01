package com.yapp.d14.user.application.service;

import com.yapp.d14.user.application.command.UserSocialLoginCommand;
import com.yapp.d14.user.application.port.in.AuthToken;
import com.yapp.d14.user.application.port.in.UserSocialLoginUseCase;
import com.yapp.d14.user.application.port.out.JwtProvider;
import com.yapp.d14.user.application.port.out.SocialAuthClient;
import com.yapp.d14.user.application.port.out.SocialUserInfo;
import com.yapp.d14.user.application.port.out.TokenRepository;
import com.yapp.d14.user.application.port.out.UserRepository;
import com.yapp.d14.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserSocialLoginService implements UserSocialLoginUseCase {

    private final SocialAuthClient socialAuthClient;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public AuthToken login(UserSocialLoginCommand command) {
        SocialUserInfo userInfo = socialAuthClient.getUserInfo(command.provider(), command.credential());

        User user = userRepository.findByProviderAndProviderId(command.provider(), userInfo.providerId())
                .orElseGet(() -> userRepository.save(
                        User.create(userInfo.email(), userInfo.name(), command.provider(), userInfo.providerId())
                ));

        String accessToken = jwtProvider.issueAccessToken(user.getId(), user.getProvider());
        String refreshToken = UUID.randomUUID().toString();
        tokenRepository.save(user.getId(), refreshToken);

        return new AuthToken(accessToken, refreshToken);
    }
}

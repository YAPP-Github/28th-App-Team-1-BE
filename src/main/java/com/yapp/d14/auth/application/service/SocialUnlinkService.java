package com.yapp.d14.auth.application.service;

import com.yapp.d14.auth.application.command.SocialUnlinkCommand;
import com.yapp.d14.auth.application.port.in.SocialUnlinkUseCase;
import com.yapp.d14.auth.application.port.out.AppleSocialClient;
import com.yapp.d14.auth.application.port.out.KakaoSocialClient;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class SocialUnlinkService implements SocialUnlinkUseCase {

    private final KakaoSocialClient kakaoSocialClient;
    private final AppleSocialClient appleSocialClient;

    @Override
    public void unlink(SocialUnlinkCommand command) {
        switch (command.provider()) {
            case KAKAO -> kakaoSocialClient.unlink(command.providerId());
            case APPLE -> revokeApple(command.appleRefreshToken());
        }
    }

    private void revokeApple(String appleRefreshToken) {
        if (appleRefreshToken == null) {
            throw new AuthException(AuthErrorCode.SOCIAL_RECONNECT_REQUIRED);
        }
        appleSocialClient.revoke(appleRefreshToken);
    }
}

package com.yapp.d14.auth.adapter.out.integration;

import com.yapp.d14.auth.application.port.out.AppleSocialClient;
import com.yapp.d14.auth.application.port.out.KakaoSocialClient;
import com.yapp.d14.auth.application.port.out.SocialAuthClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.user.domain.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SocialAuthClientAdapter implements SocialAuthClient {

    private final KakaoSocialClient kakaoSocialClient;
    private final AppleSocialClient appleSocialClient;

    @Override
    public SocialUserInfo getUserInfo(Provider provider, String credential) {
        return switch (provider) {
            case KAKAO -> kakaoSocialClient.getUserInfo(credential);
            case APPLE -> appleSocialClient.getUserInfo(credential);
        };
    }
}

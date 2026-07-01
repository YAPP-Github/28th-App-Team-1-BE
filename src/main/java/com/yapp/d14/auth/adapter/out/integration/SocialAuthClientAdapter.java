package com.yapp.d14.auth.adapter.out.integration;

import com.yapp.d14.auth.adapter.out.integration.apple.AppleAuthAdapter;
import com.yapp.d14.auth.adapter.out.integration.kakao.KakaoAuthAdapter;
import com.yapp.d14.auth.application.port.out.SocialAuthClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.user.domain.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SocialAuthClientAdapter implements SocialAuthClient {

    private final KakaoAuthAdapter kakaoAuthAdapter;
    private final AppleAuthAdapter appleAuthAdapter;

    @Override
    public SocialUserInfo getUserInfo(Provider provider, String credential) {
        return switch (provider) {
            case KAKAO -> kakaoAuthAdapter.getUserInfo(credential);
            case APPLE -> appleAuthAdapter.getUserInfo(credential);
        };
    }
}

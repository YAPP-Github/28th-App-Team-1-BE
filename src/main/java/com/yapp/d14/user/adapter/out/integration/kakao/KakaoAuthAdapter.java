package com.yapp.d14.user.adapter.out.integration.kakao;

import com.yapp.d14.user.application.port.out.SocialUserInfo;
import com.yapp.d14.user.exception.UserErrorCode;
import com.yapp.d14.user.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class KakaoAuthAdapter {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    public SocialUserInfo getUserInfo(String accessToken) {
        try {
            KakaoProfileResponse response = RestClient.create()
                    .get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoProfileResponse.class);

            String providerId = String.valueOf(response.getId());
            String email = response.getKakaoAccount() != null ? response.getKakaoAccount().getEmail() : null;
            String name = response.getKakaoAccount() != null && response.getKakaoAccount().getProfile() != null
                    ? response.getKakaoAccount().getProfile().getNickname()
                    : null;

            return new SocialUserInfo(providerId, email, name);
        } catch (Exception e) {
            log.error("[KAKAO LOGIN] 카카오 유저 정보 조회 실패", e);
            throw new UserException(UserErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }
}

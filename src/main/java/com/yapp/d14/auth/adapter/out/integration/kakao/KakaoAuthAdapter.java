package com.yapp.d14.auth.adapter.out.integration.kakao;

import com.yapp.d14.auth.application.port.out.KakaoSocialClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.common.properties.KakaoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
class KakaoAuthAdapter implements KakaoSocialClient {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    private final KakaoProperties kakaoProperties;

    @Override
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

            return new SocialUserInfo(providerId, email, name, null);
        } catch (Exception e) {
            log.error("[KAKAO LOGIN] 카카오 유저 정보 조회 실패", e);
            throw new AuthException(AuthErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }

    @Override
    public void unlink(String providerId) {
        try {
            RestClient.create()
                    .post()
                    .uri(KAKAO_UNLINK_URL)
                    .header("Authorization", "KakaoAK " + kakaoProperties.getAdminKey())
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .body("target_id_type=user_id&target_id=" + providerId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("[KAKAO UNLINK] 카카오 연결 끊기 실패: providerId={}", providerId, e);
            throw new AuthException(AuthErrorCode.SOCIAL_UNLINK_FAILED);
        }
    }
}

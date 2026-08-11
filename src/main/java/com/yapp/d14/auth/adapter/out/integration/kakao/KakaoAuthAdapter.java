package com.yapp.d14.auth.adapter.out.integration.kakao;

import com.yapp.d14.auth.application.port.out.KakaoSocialClient;
import com.yapp.d14.auth.application.port.out.SocialUserInfo;
import com.yapp.d14.auth.exception.AuthErrorCode;
import com.yapp.d14.auth.exception.AuthException;
import com.yapp.d14.common.properties.KakaoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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
        } catch (HttpClientErrorException e) {
            // 이미 앱과 연결이 끊긴 유저(-101)는 우리가 원하는 최종 상태이므로 실패가 아니라 멱등 성공으로 처리한다.
            // 그 외 카카오 오류(admin key 오류·rate limit·5xx 등)는 실제 연결이 남아있을 수 있으므로 그대로 실패시킨다.
            if (isAlreadyUnlinked(e)) {
                log.info("[KAKAO UNLINK] 이미 연결이 끊긴 유저 - 멱등 처리: providerId={}", providerId);
                return;
            }
            log.error("[KAKAO UNLINK] 카카오 연결 끊기 실패: providerId={}", providerId, e);
            throw new AuthException(AuthErrorCode.SOCIAL_UNLINK_FAILED);
        } catch (Exception e) {
            log.error("[KAKAO UNLINK] 카카오 연결 끊기 실패: providerId={}", providerId, e);
            throw new AuthException(AuthErrorCode.SOCIAL_UNLINK_FAILED);
        }
    }

    private boolean isAlreadyUnlinked(HttpClientErrorException e) {
        try {
            KakaoErrorResponse body = e.getResponseBodyAs(KakaoErrorResponse.class);
            return body != null && body.isNotRegisteredUser();
        } catch (Exception parseError) {
            log.warn("[KAKAO UNLINK] 카카오 에러 응답 파싱 실패, 실패로 처리: body={}", e.getResponseBodyAsString(), parseError);
            return false;
        }
    }
}

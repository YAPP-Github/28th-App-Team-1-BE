package com.yapp.d14.auth.adapter.out.integration.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
class KakaoProfileResponse {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    static class KakaoAccount {

        private String email;
        private Profile profile;

        @Getter
        static class Profile {
            private String nickname;
        }
    }
}

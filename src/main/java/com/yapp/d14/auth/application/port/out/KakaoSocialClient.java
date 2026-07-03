package com.yapp.d14.auth.application.port.out;

public interface KakaoSocialClient {

    SocialUserInfo getUserInfo(String accessToken);
}
